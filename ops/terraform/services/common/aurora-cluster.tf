resource "aws_db_subnet_group" "aurora_cluster" {
  description = "Subnet group for aurora cluster"
  name        = "bfd-${local.env}-aurora-cluster" # NOTE: Ephemeral environment compatible.
  subnet_ids = [
    data.aws_subnet.data[0].id,
    data.aws_subnet.data[1].id,
    data.aws_subnet.data[2].id
  ]
}

resource "aws_security_group" "aurora_cluster" {
  description            = "Security group for aurora cluster"
  name                   = "bfd-${local.env}-aurora-cluster" # NOTE: Ephemeral environment compatible.
  revoke_rules_on_delete = false
  vpc_id                 = data.aws_vpc.main.id

  egress = [
    {
      cidr_blocks = [
        "0.0.0.0/0",
      ]
      description      = ""
      from_port        = 0
      ipv6_cidr_blocks = []
      prefix_list_ids  = []
      protocol         = "-1"
      security_groups  = []
      self             = false
      to_port          = 0
    },
  ]

  tags = { Name = "bfd-${local.env}-aurora-cluster" }
}

resource "aws_rds_cluster" "aurora_cluster" {
  engine            = "aurora-postgresql"
  engine_mode       = "provisioned"
  engine_version    = "14.9"
  apply_immediately = local.rds_apply_immediately

  backtrack_window                    = 0
  backup_retention_period             = local.rds_backup_retention_period
  cluster_identifier                  = local.rds_cluster_identifier
  copy_tags_to_snapshot               = true
  db_cluster_parameter_group_name     = aws_rds_cluster_parameter_group.aurora_cluster.name
  db_instance_parameter_group_name    = aws_db_parameter_group.aurora_cluster.name
  db_subnet_group_name                = aws_db_subnet_group.aurora_cluster.name
  iam_database_authentication_enabled = local.rds_iam_database_authentication_enabled
  kms_key_id                          = data.aws_kms_key.cmk.arn
  port                                = 5432
  preferred_backup_window             = "05:00-06:00"
  preferred_maintenance_window        = "fri:07:00-fri:08:00"
  skip_final_snapshot                 = true
  storage_encrypted                   = true
  storage_type                        = "aurora-iopt1"

  # if deletion_protection_override is null, use the default value for the environment, otherwise use the override
  deletion_protection = local.rds_deletion_protection_override != null ? local.rds_deletion_protection_override : !local.is_ephemeral_env

  # TODO: consider implementing conditional inclusion of the 'cpm backup' tag
  tags = { "cpm backup" = "Weekly", "Layer" = "data" }

  # master username and password are null when a snapshot identifier is specified (clone and ephemeral support)
  master_password     = local.rds_master_password
  master_username     = local.rds_master_username
  snapshot_identifier = local.rds_snapshot_identifier

  availability_zones = [
    data.aws_availability_zones.main.names[0],
    data.aws_availability_zones.main.names[1],
    data.aws_availability_zones.main.names[2]
  ]

  enabled_cloudwatch_logs_exports = [
    "postgresql",
  ]

  vpc_security_group_ids = [
    aws_security_group.aurora_cluster.id,
    data.aws_security_group.management.id,
    data.aws_security_group.tools.id,
    data.aws_security_group.vpn.id,
  ]
}

resource "aws_rds_cluster_parameter_group" "aurora_cluster" {
  description = "Sets cluster parameters for ${local.rds_aurora_family}"
  name_prefix = "bfd-${local.env}-aurora-cluster"
  family      = local.rds_aurora_family

  dynamic "parameter" {
    for_each = local.db_cluster_parameters

    content {
      apply_method = parameter.value["apply_method"]
      name         = parameter.value["name"]
      value        = parameter.value["value"]
    }
  }
}

resource "aws_db_parameter_group" "aurora_cluster" {
  description = "Sets node parameters for ${local.rds_aurora_family}"
  family      = local.rds_aurora_family
  name_prefix = "bfd-${local.env}-aurora-node"

  dynamic "parameter" {
    for_each = local.db_parameters

    content {
      apply_method = parameter.value["apply_method"]
      name         = parameter.value["name"]
      value        = parameter.value["value"]
    }
  }
}

resource "aws_rds_cluster_instance" "nodes" {
  count                           = local.rds_instance_count
  auto_minor_version_upgrade      = false # minor cluster upgrades can cause downtime
  ca_cert_identifier              = "rds-ca-rsa4096-g1"
  cluster_identifier              = aws_rds_cluster.aurora_cluster.id
  copy_tags_to_snapshot           = true
  db_subnet_group_name            = aws_rds_cluster.aurora_cluster.db_subnet_group_name
  engine                          = aws_rds_cluster.aurora_cluster.engine
  engine_version                  = aws_rds_cluster.aurora_cluster.engine_version
  identifier                      = "${aws_rds_cluster.aurora_cluster.id}-node-${count.index}"
  instance_class                  = local.rds_instance_class
  monitoring_interval             = 15
  monitoring_role_arn             = data.aws_iam_role.monitoring.arn
  performance_insights_enabled    = true
  performance_insights_kms_key_id = aws_rds_cluster.aurora_cluster.kms_key_id
  preferred_maintenance_window    = aws_rds_cluster.aurora_cluster.preferred_maintenance_window
  publicly_accessible             = false
  tags                            = { Layer = "data" }
}

### The following configuration is almost exlcusively for separated, custom reader endpoints
### supporting development of synthea data
locals {
  # declare a reader_nodes collection for nodes that aren't currently identified as a writer
  reader_nodes = [for node in aws_rds_cluster_instance.nodes : node if !node.writer]
}

# This is the general reader endpoint in production
resource "aws_rds_cluster_endpoint" "readers" {
  # Create the separate endpoint for prod clusters
  count = local.env == "prod" ? 1 : 0

  cluster_identifier          = aws_rds_cluster.aurora_cluster.id
  cluster_endpoint_identifier = "bfd-${local.env}-ro"
  custom_endpoint_type        = "READER"

  # EXCLUDED_MEMBERS assigns ALL but the last reader to the custom endpoint
  excluded_members = [
    element(local.reader_nodes, length(local.reader_nodes) - 1).id
  ]
}

# This is the reserved synthea reader endpoint in production
resource "aws_rds_cluster_endpoint" "beta_reader" {
  # Create the separate endpoint for prod clusters
  count = local.env == "prod" ? 1 : 0

  cluster_identifier          = aws_rds_cluster.aurora_cluster.id
  cluster_endpoint_identifier = "bfd-${local.env}-beta-reader"
  custom_endpoint_type        = "READER"

  # STATIC_MEMBERS assigns just the last reader node to the custom endpoint
  static_members = [
    element(local.reader_nodes, length(local.reader_nodes) - 1).id
  ]
}
