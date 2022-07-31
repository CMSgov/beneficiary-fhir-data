resource "aws_db_subnet_group" "aurora_cluster" {
  description = "Subnet group for aurora cluster"
  name        = "bfd-${local.env}-aurora-cluster" # NOTE: Ephemeral environment compatible.
  tags        = local.shared_tags
  tags_all    = local.shared_tags

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

  tags = merge(
    local.shared_tags,
    { "Name" = "bfd-${local.env}-aurora-cluster" }
  )
  tags_all = merge(
    local.shared_tags,
    { "Name" = "bfd-${local.env}-aurora-cluster" }
  )
}

resource "aws_rds_cluster" "aurora_cluster" {
  backtrack_window                    = 0
  backup_retention_period             = local.rds_backup_retention_period
  cluster_identifier                  = local.rds_cluster_identifier
  copy_tags_to_snapshot               = true
  db_cluster_parameter_group_name     = "default.${local.rds_aurora_family}"
  db_subnet_group_name                = aws_db_subnet_group.aurora_cluster.name
  deletion_protection                 = true
  engine                              = "aurora-postgresql"
  engine_mode                         = "provisioned"
  engine_version                      = "14.3" # TODO: This may need to be ignored.
  iam_database_authentication_enabled = local.rds_iam_database_authentication_enabled
  kms_key_id                          = data.aws_kms_key.cmk.arn
  port                                = 5432
  preferred_backup_window             = "05:00-06:00"         # TODO: Review with Keith
  preferred_maintenance_window        = "fri:07:00-fri:08:00" # TODO: Review with Keith
  skip_final_snapshot                 = true
  storage_encrypted                   = true

  # credentials are null when a snapshot identifier is specified; clone and ephemeral support
  master_password     = local.rds_snapshot_identifier == null ? local.rds_master_password : null
  master_username     = local.rds_snapshot_identifier == null ? local.rds_master_username : null
  snapshot_identifier = local.rds_snapshot_identifier


  availability_zones = [
    data.aws_availability_zones.main.names[0],
    data.aws_availability_zones.main.names[1],
    data.aws_availability_zones.main.names[2]
  ]

  enabled_cloudwatch_logs_exports = [
    "postgresql",
  ]

  tags = merge(
    local.shared_tags,
    { "cpm backup" = "Weekly Monthly", "Layer" = "data" }
  )

  tags_all = merge(
    local.shared_tags,
    { "cpm backup" = "Weekly Monthly", "Layer" = "data" }
  )

  vpc_security_group_ids = [
    aws_security_group.aurora_cluster.id,
    data.aws_security_group.management.id,
    data.aws_security_group.tools.id,
    data.aws_security_group.vpn.id,
  ]
}

resource "aws_db_parameter_group" "aurora_cluster" {
  description = "Sets node parameters for ${local.rds_aurora_family}"
  family      = local.rds_aurora_family
  name_prefix = "bfd-${local.env}-aurora-node"
  tags        = local.shared_tags
  tags_all    = local.shared_tags

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
  auto_minor_version_upgrade      = true
  ca_cert_identifier              = "rds-ca-2019" # NOTE: This seems like an invariant
  cluster_identifier              = aws_rds_cluster.aurora_cluster.id
  copy_tags_to_snapshot           = true
  db_parameter_group_name         = aws_db_parameter_group.aurora_cluster.name
  db_subnet_group_name            = aws_rds_cluster.aurora_cluster.db_subnet_group_name
  engine                          = aws_rds_cluster.aurora_cluster.engine
  engine_version                  = aws_rds_cluster.aurora_cluster.engine_version # TODO: may need to ignore this...
  identifier                      = "${aws_rds_cluster.aurora_cluster.id}-node-${count.index}"
  instance_class                  = local.rds_instance_class # TODO: should this come from the aurora cluster?
  monitoring_interval             = 15                       # TODO: Does this interval need to be more granular in prod?
  monitoring_role_arn             = data.aws_iam_role.monitoring.arn
  performance_insights_enabled    = true # TODO: Is this necessary for test? Should this be on for *all* environments?
  performance_insights_kms_key_id = aws_rds_cluster.aurora_cluster.kms_key_id
  preferred_maintenance_window    = aws_rds_cluster.aurora_cluster.preferred_maintenance_window # "fri:07:00-fri:08:00" # TODO: should this come from the aurora cluster?
  publicly_accessible             = false
  tags = merge(
    local.shared_tags,
    { "Layer" = "data" }
  )

  tags_all = merge(
    local.shared_tags,
    { "Layer" = "data" }
  )
}

locals {
  # declare a reader_nodes collection for nodes that aren't currently identified as a writer
  reader_nodes = [for node in aws_rds_cluster_instance.nodes : node if !node.writer]
}

resource "aws_rds_cluster_endpoint" "readers" {
  # Create the separate endpoint for prod clusters
  count = local.env == "prod" ? 1 : 0

  cluster_identifier          = aws_rds_cluster.aurora_cluster.id
  cluster_endpoint_identifier = "bfd-${local.env}-ro"
  custom_endpoint_type        = "READER"

  # assign all but the last reader node to this endpoint
  excluded_members = [
    element(local.reader_nodes, length(local.reader_nodes) - 1).id
  ]
}

resource "aws_rds_cluster_endpoint" "beta_reader" {
  # Create the separate endpoint for prod clusters
  count = local.env == "prod" ? 1 : 0

  cluster_identifier          = aws_rds_cluster.aurora_cluster.id
  cluster_endpoint_identifier = "bfd-${local.env}-beta-reader"
  custom_endpoint_type        = "READER"

  static_members = [
    element(local.reader_nodes, length(local.reader_nodes) - 1).id
  ]
}
