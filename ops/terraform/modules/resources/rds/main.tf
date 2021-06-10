## Setup an RDS instance. Each RDS instance is deployed to specific AZ with their own dns entry.
#

locals {
  name                = "bfd-${var.env_config.env}-${var.role}"
  identifier          = local.name
  tags                = merge({ Layer = "data", role = var.role }, var.env_config.tags)
  is_prod             = substr(var.env_config.env, 0, 4) == "prod"
  deletion_protection = local.is_prod
  is_master           = var.replicate_source_db == ""
}

data "aws_iam_role" "rds_monitoring" {
  name = "rds-monitoring-role"
}


## Build a RDS with the following
#   - Encryption using a Customer Managed Key
#   - Autoscale storage size
#   - Single AZ with replication to multiple az
#   - Monitoring to cloud watch
#   - Automatic backups and maintenence windows
#   - deletition protection in prod environments
#
resource "aws_db_instance" "db" {
  allocated_storage = var.db_config.allocated_storage
  storage_type      = "io1"
  iops              = var.db_config.iops
  instance_class    = var.db_config.instance_class
  identifier        = local.identifier
  multi_az          = false
  availability_zone = var.availability_zone

  # temp db to get started
  name                            = local.is_master ? "bfdtemp" : null
  username                        = local.is_master ? "bfduser" : null
  password                        = local.is_master ? "changeme!" : null
  kms_key_id                      = var.kms_key_id
  db_subnet_group_name            = local.is_master ? var.subnet_group : null
  vpc_security_group_ids          = var.vpc_security_group_ids
  tags                            = local.tags
  monitoring_interval             = 15
  monitoring_role_arn             = data.aws_iam_role.rds_monitoring.arn
  engine                          = "postgres"
  engine_version                  = "9.6.11"
  storage_encrypted               = true
  copy_tags_to_snapshot           = true
  skip_final_snapshot             = ! local.deletion_protection
  deletion_protection             = local.deletion_protection
  replicate_source_db             = local.is_master ? "" : var.replicate_source_db
  maintenance_window              = "Fri:07:00-Fri:08:00"    # 3 am EST
  backup_window                   = "05:00-06:00"            # 1 am EST
  backup_retention_period         = local.is_master ? 21 : 0 # 3 ETL periods instead of the default 7 days
  performance_insights_enabled    = false                    # Not supported in postgres 9.6
  final_snapshot_identifier       = local.deletion_protection ? "${local.name}-final" : null
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"] # Could add 'listener' and "audit"

  # depends on the state of var.db_import_mode.enabled in the parent module
  parameter_group_name = var.parameter_group_name
  apply_immediately    = var.apply_immediately

  lifecycle {
    ignore_changes = ["password"]
  }
}

resource "aws_route53_record" "db" {
  name    = var.role
  type    = "CNAME"
  zone_id = var.env_config.zone_id
  ttl     = "300"

  records = [aws_db_instance.db.address]
}
