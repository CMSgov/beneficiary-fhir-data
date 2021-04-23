locals {
  # note: this captures both prod and prod-sbx
  is_prod = substr(var.env_config.env, 0, 4) == "prod"

  # see https://github.com/CMSgov/beneficiary-fhir-data/pull/476 for reference
  # on why prod's naming is different
  node_identifier = {
    "prod" = "bfd-prod-aurora-cluster",
    "prod-sbx" = "bfd-prod-sbx-aurora",
    "test" = "bfd-test-aurora"
  }
}

data "aws_iam_role" "rds_monitoring" {
  name = "rds-monitoring-role"
}

resource "aws_security_group" "aurora_cluster" {
  name        = "bfd-${var.env_config.env}-aurora-cluster"
  description = "Security group for aurora cluster"
  vpc_id      = var.env_config.vpc_id

  # Ingress will come from security group rules defined stateless module
  egress {
    from_port = 0
    protocol  = "-1"
    to_port   = 0

    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge({ Name = "bfd-${var.env_config.env}-aurora-cluster" }, var.env_config.tags)
}

resource "aws_db_subnet_group" "aurora_cluster" {
  name        = "bfd-${var.env_config.env}-aurora-cluster"
  description = "Subnet group for aurora cluster"
  subnet_ids  = var.stateful_config.subnet_ids

  tags = var.env_config.tags
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

resource "random_password" "aurora_cluster" {
  length  = 20
  special = false
}

resource "aws_rds_cluster" "aurora_cluster" {
  cluster_identifier = "bfd-${var.env_config.env}-aurora-cluster"

  availability_zones     = var.stateful_config.azs
  db_subnet_group_name   = aws_db_subnet_group.aurora_cluster.name
  vpc_security_group_ids = concat([aws_security_group.aurora_cluster.id], var.stateful_config.vpc_sg_ids)

  storage_encrypted = true
  kms_key_id        = var.stateful_config.kms_key_id

  engine         = "aurora-postgresql"
  engine_version = var.aurora_config.engine_version

  deletion_protection       = local.is_prod
  skip_final_snapshot       = ! local.is_prod
  final_snapshot_identifier = local.is_prod ? "bfd-${var.env_config.env}-aurora-cluster-final" : null

  backup_retention_period      = 21
  preferred_backup_window      = "05:00-06:00"         # 1 am EST
  preferred_maintenance_window = "Fri:07:00-Fri:08:00" # 3 am EST

  enabled_cloudwatch_logs_exports = [
    "postgresql"
  ]

  master_username = "bfduser"
  master_password = random_password.aurora_cluster.result

  tags                  = merge({ Layer = "data" }, var.env_config.tags)
  copy_tags_to_snapshot = true

  lifecycle {
    ignore_changes = ["master_password"]
  }
}

resource "aws_rds_cluster_instance" "aurora_nodes" {
  count          = var.aurora_config.cluster_nodes
  instance_class = var.aurora_config.instance_class

  identifier         = "${lookup(local.node_identifier, var.env_config.env)}-node-${count.index}"
  cluster_identifier = aws_rds_cluster.aurora_cluster.id

  # note: element(list,index) function wraps. e.g., element([az1,az2,az3],3) == az1
  availability_zone       = element(var.stateful_config.azs, count.index)
  db_subnet_group_name    = aws_db_subnet_group.aurora_cluster.name
  db_parameter_group_name = aws_db_parameter_group.aurora_node.name

  engine         = "aurora-postgresql"
  engine_version = var.aurora_config.engine_version

  monitoring_role_arn = data.aws_iam_role.rds_monitoring.arn
  monitoring_interval = 15

  preferred_maintenance_window = "Fri:07:00-Fri:08:00" # 3 am EST

  performance_insights_enabled    = true
  performance_insights_kms_key_id = var.stateful_config.kms_key_id

  tags                  = merge({ Layer = "data" }, var.env_config.tags)
  copy_tags_to_snapshot = true
}

resource "aws_route53_record" "aurora_writer" {
  name    = "aurora-writer"
  type    = "CNAME"
  zone_id = var.env_config.zone_id
  ttl     = "300"

  records = [aws_rds_cluster.aurora_cluster.endpoint]
}

resource "aws_route53_record" "aurora_reader" {
  name    = "aurora-reader"
  type    = "CNAME"
  zone_id = var.env_config.zone_id
  ttl     = "300"

  records = [aws_rds_cluster.aurora_cluster.reader_endpoint]
}

// If beta_reader toggle is true, it means we have added an extra reader node
// to the cluster for testing purposes (e.g., so we can test against prod without
// impacting prod performance). This means we want to create two custom reader
// endpoints: one for normal traffic (containing all but one reader node) and
// a "beta", aka test, reader node containing one reader node only. This assume
// we have adjusted the number of cluster nodes accordingly (which is defined
// within each environments aurora_config.cluster_nodes variable.

// this endpoint contains all but one reader node (the last node) and is behind a feature toggle
resource "aws_rds_cluster_endpoint" "readers" {
  count = var.module_features.beta_reader ? 1 : 0

  cluster_identifier          = aws_rds_cluster.aurora_cluster.id
  cluster_endpoint_identifier = "bfd-${ var.env_config.env }-ro"
  custom_endpoint_type        = "READER"

  // assign all but the last reader node to this endpoint
  excluded_members = [
    element(aws_rds_cluster_instance.aurora_nodes, length(aws_rds_cluster_instance.aurora_nodes) - 1).id
  ]
}

// this endpoint only contains one reader node (the last node) and is behind a feature toggle
resource "aws_rds_cluster_endpoint" "beta_reader" {
  count = var.module_features.beta_reader ? 1 : 0

  cluster_identifier          = aws_rds_cluster.aurora_cluster.id
  cluster_endpoint_identifier = "bfd-${ var.env_config.env }-beta-reader"
  custom_endpoint_type        = "READER"

  // assign all but the last reader node to this endpoint
  static_members = [
    element(aws_rds_cluster_instance.aurora_nodes, length(aws_rds_cluster_instance.aurora_nodes) - 1).id
  ]
}
