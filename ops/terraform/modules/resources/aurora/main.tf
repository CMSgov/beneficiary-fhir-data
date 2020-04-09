locals {
  tags    = merge({Layer="data"}, var.env_config.tags)
  is_prod = substr(var.env_config.env, 0, 4) == "prod" 
}

data "aws_iam_role" "rds_monitoring" {
  name = "rds-monitoring-role"
}

resource "aws_db_parameter_group" "aurora_node" {
  name        = "bfd-${var.env_config.env}-aurora-node"
  family      = var.aurora_config.param_version
  description = "Sets node parameters for ${var.aurora_config.param_version}"

  dynamic "parameter" {
    for_each = var.aurora_node_params
    content {
      name         = parameter.value.name
      value        = parameter.value.value
      apply_method = parameter.value.apply_on_reboot ? "pending-reboot" : null
    }
  }
}

resource "aws_rds_cluster" "aurora_cluster" {
  cluster_identifier = "bfd-${var.env_config.env}-aurora-cluster"

  availability_zones     = var.stateful_config.azs
  db_subnet_group_name   = var.stateful_config.subnet_group
  vpc_security_group_ids = var.stateful_config.vpc_sg_ids

  storage_encrypted = true
  kms_key_id        = var.stateful_config.kms_key_id

  engine         = "aurora-postgresql"
  engine_version = var.aurora_config.engine_version

  deletion_protection       = local.is_prod
  skip_final_snapshot       = !local.is_prod
  final_snapshot_identifier = local.is_prod ? "${local.name}-final" : null

  backup_retention_period      = 21
  preferred_backup_window      = "05:00-06:00"  # 1 am EST
  preferred_maintenance_window = "Fri:07:00-Fri:08:00"  # 3 am EST

  enabled_cloudwatch_logs_exports = [
    "audit",
    "error",
    "general",
    "slowquery",
    "postgresql"
  ]

  master_username = "bfduser"
  master_password = "changeme!"

  tags                  = merge({Layer="data"}, local.env_config.tags)
  copy_tags_to_snapshot = true

  lifecycle {
    ignore_changes = ["master_password"]
  }
}

resource "aws_rds_cluster_instance" "aurora_nodes" {
  count          = var.aurora_config.cluster_nodes
  instance_class = var.aurora_config.instance_class

  identifier         = "bfd-${var.env_config.env}-aurora-node-${count.index}"
  cluster_identifier = aws_rds_cluster.aurora_cluster.id

  availability_zone       = var.stateful_config.azs[count.index]
  db_subnet_group_name    = var.stateful_config.subnet_group
  db_parameter_group_name = aws_db_parameter_group.aurora_node.name

  engine         = "aurora-postgresql"
  engine_version = var.aurora_config.engine_version

  monitoring_role_arn  = data.aws_iam_role.rds_monitoring.arn
  monitoring_interval  = 15

  preferred_backup_window      = "05:00-06:00"  # 1 am EST
  preferred_maintenance_window = "Fri:07:00-Fri:08:00"  # 3 am EST

  performance_insights_enabled    = true
  performance_insights_kms_key_id = var.stateful_config.kms_key_id

  tags                  = merge({Layer="data"}, local.env_config.tags)
  copy_tags_to_snapshot = true
}
