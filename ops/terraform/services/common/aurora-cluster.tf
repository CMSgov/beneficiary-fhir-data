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
  allow_major_version_upgrade = false
  engine                      = "aurora-postgresql"
  engine_mode                 = "provisioned"
  engine_version              = "16.4"
  apply_immediately           = local.rds_apply_immediately

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

  tags = { "cpm backup" = local.is_ephemeral_env ? "" : "Weekly", "Layer" = "data" }

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

  # Autoscaled reader nodes, by default, are not configured with Performance Insights. Until
  # recently, the only option for enabling Performance Insights for those nodes would be to enable
  # it after they scale-out and reach the "available" state. However, it seems that it is now
  # possible to enable both Performance Insights and Enhanced Monitoring at the Cluster level for
  # Aurora Clusters, avoiding such a workaround. Unfortunately, the Terraform AWS Provider does not
  # properly support enabling both Performance Insights and Enhanced Monitoring at the Cluster level
  # as of 01/25, thus necessitating this local-exec provisioner. Fortunately, once enabled, the
  # settings for these cannot be changed, so we only need them to be enabled at creation-time.
  provisioner "local-exec" {
    environment = {
      DB_CLUSTER_ID                    = self.cluster_identifier
      KMS_KEY_ID                       = self.kms_key_id
      ENHANCED_MONITORING_INTERVAL     = 15
      ENHANCED_MONITORING_IAM_ROLE_ARN = data.aws_iam_role.monitoring.arn
    }
    command     = <<-EOF
    aws rds modify-db-cluster --db-cluster-identifier "$DB_CLUSTER_ID" \
      --performance-insights-kms-key-id "$KMS_KEY_ID" \
      --enable-performance-insights \
      --monitoring-interval "$ENHANCED_MONITORING_INTERVAL" \
      --monitoring-role-arn "$ENHANCED_MONITORING_IAM_ROLE_ARN" 1>/dev/null &&
      echo "Performance Insights and Enhanced Monitoring enabled for $DB_CLUSTER_ID"
    EOF
    interpreter = ["/bin/bash", "-c"]
  }

  # Autoscaled reader nodes are not managed by Terraform and Terraform is unable to destroy a
  # cluster with nodes still within it. To support simply running "terraform destroy" in
  # environments with autoscaling enabled, a helper script is used that will automatically mark all
  # autoscaled nodes for deletion and wait for them to be deleted before exiting. This runs only on
  # destroy
  provisioner "local-exec" {
    when        = destroy
    command     = "${path.module}/scripts/destroy-autoscaled-nodes.sh"
    interpreter = ["/bin/bash", "-c"]
    environment = {
      DB_CLUSTER_ID = self.cluster_identifier
      # This may seem strange, but provisioners can only refer to properties of the resource which
      # local.env is not. Fortunately, the "stack" tag _is_ the name of the current environment, so
      # we can use that to pass the environment to the script
      BFD_ENVIRONMENT = self.tags_all["stack"]
    }
  }
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

resource "aws_rds_cluster_instance" "writer" {
  auto_minor_version_upgrade   = true
  ca_cert_identifier           = "rds-ca-rsa4096-g1"
  cluster_identifier           = aws_rds_cluster.aurora_cluster.id
  copy_tags_to_snapshot        = true
  db_subnet_group_name         = aws_rds_cluster.aurora_cluster.db_subnet_group_name
  db_parameter_group_name      = aws_rds_cluster.aurora_cluster.db_instance_parameter_group_name
  engine                       = aws_rds_cluster.aurora_cluster.engine
  engine_version               = aws_rds_cluster.aurora_cluster.engine_version
  identifier                   = "${aws_rds_cluster.aurora_cluster.id}-writer-node"
  instance_class               = local.rds_instance_class
  preferred_maintenance_window = aws_rds_cluster.aurora_cluster.preferred_maintenance_window
  publicly_accessible          = false
  tags                         = { Layer = "data" }
}
