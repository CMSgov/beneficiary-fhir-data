terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  service              = local.service
  relative_module_root = "ops/services/02-database"
  subnet_layers        = ["private"]
}

locals {
  service = "database"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = module.terraservice.ssm_config
  env_key_alias            = module.terraservice.env_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  azs                      = module.terraservice.default_azs
  data_subnets             = module.terraservice.subnets_map["private"]
  external_sgs             = [module.terraservice.cms_cloud_vpn_sg]

  rds_aurora_family                       = "aurora-postgresql16"
  rds_cluster_id                          = nonsensitive(local.ssm_config["/bfd/database/rds_cluster_identifier"])
  rds_cluster_sg                          = nonsensitive(local.ssm_config["/bfd/database/rds_security_group"])
  rds_iam_database_authentication_enabled = nonsensitive(local.ssm_config["/bfd/database/rds_iam_database_authentication_enabled"])
  rds_instance_class                      = nonsensitive(local.ssm_config["/bfd/database/rds_instance_class"])
  rds_min_reader_nodes                    = nonsensitive(local.ssm_config["/bfd/database/scaling/min_nodes"])
  rds_max_reader_nodes                    = nonsensitive(local.ssm_config["/bfd/database/scaling/max_nodes"])
  rds_scaling_cpu_target                  = nonsensitive(local.ssm_config["/bfd/database/scaling/cpu_target"])
  rds_scale_in_cooldown                   = nonsensitive(local.ssm_config["/bfd/database/scaling/cooldown/scale_in"])
  rds_scale_out_cooldown                  = nonsensitive(local.ssm_config["/bfd/database/scaling/cooldown/scale_out"])
  rds_snapshot_identifier                 = one(data.aws_db_cluster_snapshot.main[*].id)
  rds_master_password                     = lookup(local.ssm_config, "/bfd/database/rds_master_password", null)
  rds_master_username                     = nonsensitive(lookup(local.ssm_config, "/bfd/database/rds_master_username", sensitive(null)))

  db_cluster_parameter_group_file = fileexists("${path.module}/db-cluster-parameters/${local.env}.yaml") ? "${path.module}/db-cluster-parameters/${local.env}.yaml" : "${path.module}/db-cluster-parameters/${local.rds_aurora_family}.yaml"
  db_node_parameter_group_file    = fileexists("${path.module}/db-node-parameters/${local.env}.yaml") ? "${path.module}/db-node-parameters/${local.env}.yaml" : "${path.module}/db-node-parameters/${local.rds_aurora_family}.yaml"
  db_cluster_parameters           = toset(yamldecode(file(local.db_cluster_parameter_group_file)))
  db_parameters                   = toset(yamldecode(file(local.db_node_parameter_group_file)))

  enable_rds_scheduled_scaling = !var.disable_rds_scheduling_override && (local.env == "test" || local.is_ephemeral_env)
  replicas_scaling_target      = local.enable_rds_scheduled_scaling ? one(aws_appautoscaling_target.dynamic_replicas) : one(aws_appautoscaling_target.static_replicas)

  monitoring_interval = 15
}

data "aws_db_cluster_snapshot" "main" {
  count = local.is_ephemeral_env ? 1 : 0

  db_cluster_identifier = "bfd-${local.parent_env}-aurora-cluster"

  most_recent                    = var.ephemeral_rds_snapshot_id_override == null
  db_cluster_snapshot_identifier = var.ephemeral_rds_snapshot_id_override
}

resource "aws_db_subnet_group" "this" {
  description = "Subnet group for aurora cluster"
  name        = local.rds_cluster_id
  subnet_ids  = [for x in local.data_subnets : x.id]
}

resource "aws_security_group" "this" {
  description            = "Security group for aurora cluster"
  name                   = local.rds_cluster_sg
  revoke_rules_on_delete = true
  vpc_id                 = local.vpc.id

  tags = { Name = local.rds_cluster_sg }
}

resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_rds_cluster_parameter_group" "this" {
  description = "Sets cluster parameters for ${local.rds_aurora_family}"
  name_prefix = local.rds_cluster_id
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

resource "aws_db_parameter_group" "this" {
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

resource "aws_rds_cluster" "this" {
  # Ignore all changes to these properties as the below local-exec(s) manages them. Yes, this makes
  # it so that manual changes to these values are ignored, but setting them via Terraform does not
  # work properly anyways
  lifecycle {
    ignore_changes = [
      # We ignore engine_version so that auto minor version upgrades do not cause apply failures
      engine_version,
      # The below are ignored due to the AWS Provider not properly supporting them on Aurora
      # Postgres Clusters
      monitoring_interval,
      monitoring_role_arn,
      performance_insights_enabled,
      performance_insights_kms_key_id,
      performance_insights_retention_period,
    ]
  }

  allow_major_version_upgrade = false
  engine                      = "aurora-postgresql"
  engine_mode                 = "provisioned"
  engine_version              = "16.8"
  apply_immediately           = false

  backtrack_window                    = 0
  cluster_identifier                  = local.rds_cluster_id
  copy_tags_to_snapshot               = true
  db_cluster_parameter_group_name     = aws_rds_cluster_parameter_group.this.name
  db_instance_parameter_group_name    = aws_db_parameter_group.this.name
  db_subnet_group_name                = aws_db_subnet_group.this.name
  iam_database_authentication_enabled = local.rds_iam_database_authentication_enabled
  kms_key_id                          = local.env_key_arn
  port                                = 5432
  preferred_maintenance_window        = "fri:07:00-fri:08:00"
  skip_final_snapshot                 = true
  storage_encrypted                   = true
  storage_type                        = "aurora-iopt1"

  # if deletion_protection_override is null, use the default value for the environment, otherwise use the override
  deletion_protection = !local.is_ephemeral_env

  tags = { "bfd_backup" = local.is_ephemeral_env ? "" : "daily3_weekly35", "Layer" = "data", "AWS_Backup" = "self_backup" }

  # master username and password are null when a snapshot identifier is specified (clone and ephemeral support)
  master_password     = local.rds_master_password
  master_username     = local.rds_master_username
  snapshot_identifier = local.rds_snapshot_identifier

  availability_zones = [for _, v in local.azs : v.name]

  enabled_cloudwatch_logs_exports = [
    "postgresql",
  ]

  vpc_security_group_ids = concat(
    [aws_security_group.this.id],
    local.external_sgs[*].id
  )

  # Autoscaled reader nodes, by default, are not configured with Performance Insights. Until
  # recently, the only option for enabling Performance Insights for those nodes would be to enable
  # it after they scale-out and reach the "available" state. However, it seems that it is now
  # possible to enable both Performance Insights and Enhanced Monitoring at the Cluster level for
  # Aurora Clusters, avoiding such a workaround. Unfortunately, the Terraform AWS Provider does not
  # properly support enabling both Performance Insights and Enhanced Monitoring at the Cluster level
  # as of 05/23 (specifically throwing an error indicating that enabling both is only fully
  # supported for Aurora Limitless DBs), thus necessitating this local-exec provisioner.
  # Fortunately, once enabled, the settings for these cannot be changed, so we only need them to be
  # enabled at creation-time.
  provisioner "local-exec" {
    environment = {
      DB_CLUSTER_ID                    = self.cluster_identifier
      KMS_KEY_ID                       = self.kms_key_id
      ENHANCED_MONITORING_INTERVAL     = local.monitoring_interval
      ENHANCED_MONITORING_IAM_ROLE_ARN = aws_iam_role.db_monitoring.arn
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

resource "aws_rds_cluster_instance" "writer" {
  lifecycle {
    ignore_changes = [
      # We ignore this so that auto minor version upgrades do not cause apply failures
      engine_version,
      # monitoring_interval is inherited from the cluster
      monitoring_interval
    ]
  }

  auto_minor_version_upgrade   = true
  ca_cert_identifier           = "rds-ca-rsa4096-g1"
  cluster_identifier           = aws_rds_cluster.this.id
  copy_tags_to_snapshot        = true
  db_subnet_group_name         = aws_rds_cluster.this.db_subnet_group_name
  db_parameter_group_name      = aws_rds_cluster.this.db_instance_parameter_group_name
  engine                       = aws_rds_cluster.this.engine
  engine_version               = aws_rds_cluster.this.engine_version
  identifier                   = "${aws_rds_cluster.this.id}-writer-node"
  instance_class               = local.rds_instance_class
  preferred_maintenance_window = aws_rds_cluster.this.preferred_maintenance_window
  publicly_accessible          = false
  tags                         = { Layer = "data" }
}

resource "aws_appautoscaling_target" "dynamic_replicas" {
  # All app autoscaling resources need to be applied after the writer exists or read replicas will
  # not automatically be created if the min_capacity is not satisfied
  depends_on = [aws_rds_cluster_instance.writer]

  # only applicable for test environment or ephemeral environments unless override is specified
  count = local.enable_rds_scheduled_scaling ? 1 : 0

  service_namespace  = "rds"
  scalable_dimension = "rds:cluster:ReadReplicaCount"
  resource_id        = "cluster:${aws_rds_cluster.this.id}"

  # NOTE: These are the initial, additional capacity settings for the target. Scaling events by schedule and
  # policy directly update these values on the app autoscaling target. As a result, changes to either
  # min_capacity and max_capacity fields are ignored by the lifecycle meta argument below.
  min_capacity = local.rds_min_reader_nodes
  max_capacity = local.rds_max_reader_nodes

  lifecycle {
    ignore_changes = [
      min_capacity,
      max_capacity
    ]
  }
}

resource "aws_appautoscaling_target" "static_replicas" {
  depends_on = [aws_rds_cluster_instance.writer]

  # only applicable for prod/prod-sbx/sandbox (not test or ephemeral)
  count = !local.enable_rds_scheduled_scaling ? 1 : 0

  service_namespace  = "rds"
  scalable_dimension = "rds:cluster:ReadReplicaCount"
  resource_id        = "cluster:${aws_rds_cluster.this.id}"

  min_capacity = local.rds_min_reader_nodes
  max_capacity = local.rds_max_reader_nodes
}

resource "aws_appautoscaling_policy" "replicas_cpu_scaling" {
  depends_on = [aws_rds_cluster_instance.writer]

  name               = "bfd-${local.env}-cpu-scaling"
  service_namespace  = local.replicas_scaling_target.service_namespace
  resource_id        = local.replicas_scaling_target.resource_id
  scalable_dimension = local.replicas_scaling_target.scalable_dimension
  policy_type        = "TargetTrackingScaling"

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "RDSReaderAverageCPUUtilization"
    }

    target_value       = local.rds_scaling_cpu_target
    scale_in_cooldown  = local.rds_scale_in_cooldown
    scale_out_cooldown = local.rds_scale_out_cooldown
  }
}

resource "aws_appautoscaling_scheduled_action" "work_hours_scale_out" {
  depends_on = [aws_rds_cluster_instance.writer]

  count = local.enable_rds_scheduled_scaling ? 1 : 0

  name               = "bfd-${local.env}-work-hours-scale-out"
  service_namespace  = local.replicas_scaling_target.service_namespace
  resource_id        = local.replicas_scaling_target.resource_id
  scalable_dimension = local.replicas_scaling_target.scalable_dimension
  schedule           = "cron(00 07 ? * MON-FRI *)"
  timezone           = "America/New_York"

  # NOTE: min_capacity and max_capacity will count nodes that are and
  # are not managed by app-autoscaling when calculating the desired capacity.
  scalable_target_action {
    min_capacity = local.rds_min_reader_nodes
    max_capacity = local.rds_max_reader_nodes
  }
}

resource "aws_appautoscaling_scheduled_action" "off_hours_scale_in" {
  depends_on = [aws_rds_cluster_instance.writer]

  count = local.enable_rds_scheduled_scaling ? 1 : 0

  name               = "bfd-${local.env}-off-hours-scale-in"
  service_namespace  = local.replicas_scaling_target.service_namespace
  resource_id        = local.replicas_scaling_target.resource_id
  scalable_dimension = local.replicas_scaling_target.scalable_dimension
  schedule           = "cron(00 19 ? * MON-FRI *)"
  timezone           = "America/New_York"

  # NOTE: min_capacity and max_capacity only impacts nodes managed by app-autoscaling
  scalable_target_action {
    min_capacity = 0
    max_capacity = 0
  }
}
