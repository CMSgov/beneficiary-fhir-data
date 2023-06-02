locals {
  env              = terraform.workspace
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", local.env))])
  is_ephemeral_env = !(contains(local.established_envs, local.env))
  is_prod          = local.env == "prod"

  account_id        = data.aws_caller_identity.current.account_id
  layer             = "data"
  create_etl_user   = local.is_prod || var.force_etl_user_creation
  create_slis       = contains(local.established_envs, local.env) || var.force_sli_creation
  create_dashboard  = contains(local.established_envs, local.env) || var.force_dashboard_creation
  create_slo_alarms = (contains(local.established_envs, local.env) || var.force_slo_alarms_creation) && local.create_slis
  jdbc_suffix       = var.jdbc_suffix

  # NOTE: Some resources use a 'pipeline' name while others use 'etl'. There's no simple solution for renaming all resources.
  # We must tolerate this for now.
  service        = "pipeline"
  legacy_service = "etl"

  default_tags = {
    Environment    = local.seed_env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/pipeline"
  }

  # NOTE: nonsensitive service-oriented and common config
  nonsensitive_common_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_common.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values))
  nonsensitive_common_config = { for key, value in local.nonsensitive_common_map : split("/", key)[5] => value }

  nonsensitive_ccw_service_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_ccw.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_ccw.values))
  nonsensitive_ccw_service_config = { for key, value in local.nonsensitive_ccw_service_map : split("/", key)[6] => value }

  nonsensitive_rda_service_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_rda.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_rda.values))
  nonsensitive_rda_service_config = { for key, value in local.nonsensitive_rda_service_map : split("/", key)[6] => value }

  logging_bucket  = "bfd-${local.env}-logs-${local.account_id}"
  pipeline_bucket = "bfd-${local.env}-etl-${local.account_id}"

  # SSM-derived network, security group values
  enterprise_tools_security_group = local.nonsensitive_common_config["enterprise_tools_security_group"]
  vpn_security_group              = local.nonsensitive_common_config["vpn_security_group"]
  vpc_name                        = local.nonsensitive_common_config["vpc_name"]

  # Cloudwatch Settings
  pipeline_messages_error = {
    period       = "300"
    eval_periods = "10"
    threshold    = "0"
    datapoints   = "10"
  }

  pipeline_messages_datasetfailed = {
    period       = "300"
    eval_periods = "1"
    threshold    = "0"
    datapoints   = "1"
  }

  pipeline_log_availability = {
    period       = 1 * 60 * 60 # 1 hour 
    eval_periods = 1
    threshold    = 0
    datapoints   = 1
  }

  # Used by alarms for RDA claim ingestion latency metrics.  Metric time unit is milliseconds.
  # 28800000 ms == 8 hours
  rda_pipeline_latency_alert = {
    period       = "300"
    eval_periods = "1"
    threshold    = "28800000"
    datapoints   = "1"
    metrics = local.is_ephemeral_env ? [] : [
      { sink_name = "FissClaimRdaSink", claim_type = "fiss" },
      { sink_name = "McsClaimRdaSink", claim_type = "mcs" },
    ]
  }

  log_groups = {
    messages = "/bfd/${local.env}/bfd-pipeline/messages.txt"
  }

  alarm_actions = local.is_prod ? [data.aws_sns_topic.alarm[0].arn] : []
  ok_actions    = local.is_prod ? [data.aws_sns_topic.ok[0].arn] : []

  # The log availability alarm will post an incident in prod; in other envs it will get posted
  # to #bfd-test 
  # TODO: Replace testing SNS topic in BFD-2244
  log_availability_alarm_actions = local.is_ephemeral_env ? [] : local.is_prod ? [data.aws_sns_topic.alarm[0].arn] : [data.aws_sns_topic.bfd_test_slack_alarm[0].arn]

  # The max claim latency alarm sends notifications to #bfd-notices upon entering the ALARM state
  max_claim_latency_alarm_actions = local.is_ephemeral_env ? [] : [data.aws_sns_topic.bfd_notices_slack_alarm[0].arn]

  # data-source resolution
  mgmt_kms_key_arn      = data.aws_kms_key.mgmt_cmk.arn
  ami_id                = data.aws_ami.main.image_id
  availability_zone     = data.external.rds.result["WriterAZ"]
  kms_key_id            = data.aws_kms_key.cmk.arn
  rds_security_group_id = data.aws_security_group.rds.id
  rds_writer_endpoint   = data.external.rds.result["Endpoint"]
  vpc_id                = data.aws_vpc.main.id
  vpn_security_group_id = data.aws_security_group.vpn.id
  ent_tools_sg_id       = data.aws_security_group.enterprise_tools.id
  subnet_id             = data.aws_subnet.main.id

  # pipeline specific configrations
  pipeline_variant_configs = {
    rda = {
      enabled       = var.create_rda_pipeline
      name          = "bfd-${local.env}-${local.service}-rda"
      instance_name = "rda"
      instance_type = local.nonsensitive_rda_service_config["instance_type"]
      tags = {
        Name = "bfd-${local.env}-${local.service}-rda"
      }
    }
    ccw = {
      enabled       = var.create_ccw_pipeline
      name          = "bfd-${local.env}-${local.service}-ccw"
      instance_name = "ccw"
      instance_type = local.nonsensitive_ccw_service_config["instance_type"]
      tags = {
        Name = "bfd-${local.env}-${local.service}-ccw"
      }
    }
  }
  # TODO: Consider replacing with merged map with all variants if RDA variant is updated to be on-demand
  rda_pipeline_config = {
    for k, v in local.pipeline_variant_configs : k => local.pipeline_variant_configs[k]
    if local.pipeline_variant_configs[k].enabled && k == "rda"
  }
  # TODO: Consider replacing with merged map with all variants if RDA variant is updated to be on-demand
  ccw_pipeline_config = {
    for k, v in local.pipeline_variant_configs : k => local.pipeline_variant_configs[k]
    if local.pipeline_variant_configs[k].enabled && k == "ccw"
  }
}

# TODO: Determine if resource could be consolidated with RDA variant if RDA becomes on-demand
resource "aws_launch_template" "this" {
  for_each = local.ccw_pipeline_config

  name          = each.value.name
  description   = "Template for the ${local.env} environment ${each.key} ${local.service} servers"
  key_name      = local.nonsensitive_common_config["key_pair"]
  image_id      = local.ami_id
  instance_type = each.value.instance_type

  user_data = base64encode(templatefile("${path.module}/user-data.sh.tftpl", {
    account_id        = local.account_id
    env               = local.env
    pipeline_bucket   = aws_s3_bucket.this.bucket
    pipeline_instance = each.value.instance_name
    writer_endpoint   = "jdbc:postgresql://${local.rds_writer_endpoint}:5432/fhirdb${local.jdbc_suffix}"
  }))

  instance_initiated_shutdown_behavior = "stop"
  disable_api_termination              = false
  ebs_optimized                        = true

  iam_instance_profile {
    name = aws_iam_instance_profile.this.name
  }

  network_interfaces {
    associate_carrier_ip_address = false
    security_groups              = [aws_security_group.app.id, local.vpn_security_group_id, local.ent_tools_sg_id]
  }

  placement {
    tenancy = "default"
  }

  monitoring {
    enabled = true
  }

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_type           = "gp2"
      volume_size           = 1000
      delete_on_termination = true
      encrypted             = true
      kms_key_id            = local.kms_key_id
    }
  }

  capacity_reservation_specification {
    capacity_reservation_preference = "open"
  }

  enclave_options {
    enabled = false
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 1
    http_tokens                 = "required"
  }

  dynamic "tag_specifications" {
    for_each = toset(["instance", "volume"])

    content {
      resource_type = tag_specifications.value
      tags = merge(
        local.default_tags,
        {
          Layer    = local.layer
          role     = local.legacy_service
          snapshot = true
        },
        each.value.tags
      )
    }
  }
}

# TODO: Determine if resource could be consolidated with RDA variant if RDA becomes on-demand
resource "aws_autoscaling_group" "this" {
  for_each = local.ccw_pipeline_config

  name                = each.value.name
  vpc_zone_identifier = [data.aws_subnet.main.id]
  desired_capacity    = 0
  max_size            = 1
  min_size            = 0

  health_check_grace_period = 300
  health_check_type         = "EC2"

  launch_template {
    name    = aws_launch_template.this[each.key].name
    version = aws_launch_template.this[each.key].latest_version
  }

  instance_refresh {
    strategy = "Rolling"
    # NOTE: Changes in launch_template will _always_ trigger an instance refresh, and including it
    # explicitly causes Terraform to emit a warning upon validation
    triggers = ["tag"]
  }

  dynamic "tag" {
    for_each = merge(
      local.default_tags,
      {
        Layer = local.layer,
        role  = local.legacy_service
      },
      each.value.tags
    )
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }
}

# TODO: Determine if this can be consolidated with the CCW Pipeline's infrastructure
resource "aws_instance" "pipeline" {
  for_each = { for server in local.rda_pipeline_config : server.instance_name => server }

  ami                                  = local.ami_id
  associate_public_ip_address          = false
  availability_zone                    = local.availability_zone
  disable_api_termination              = false
  ebs_optimized                        = true
  iam_instance_profile                 = aws_iam_instance_profile.this.name
  instance_initiated_shutdown_behavior = "stop"
  instance_type                        = each.value.instance_type
  key_name                             = local.nonsensitive_common_config["key_pair"]
  monitoring                           = true
  secondary_private_ips                = []
  source_dest_check                    = true
  subnet_id                            = local.subnet_id
  tags = merge(
    {
      Layer    = local.layer
      role     = local.legacy_service
      snapshot = true
    },
    each.value.tags
  )

  tenancy = "default"

  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    account_id        = local.account_id
    env               = local.env
    pipeline_bucket   = aws_s3_bucket.this.bucket
    pipeline_instance = each.value.instance_name
    writer_endpoint   = "jdbc:postgresql://${local.rds_writer_endpoint}:5432/fhirdb${local.jdbc_suffix}"
  })

  volume_tags = merge(
    local.default_tags,
    {
      Layer    = local.layer
      role     = local.legacy_service
      snapshot = true
    },
    each.value.tags
  )

  vpc_security_group_ids = [
    aws_security_group.app.id,
    local.vpn_security_group_id,
    local.ent_tools_sg_id
  ]

  capacity_reservation_specification {
    capacity_reservation_preference = "open"
  }

  enclave_options {
    enabled = false
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 1
    http_tokens                 = "required"
  }

  root_block_device {
    delete_on_termination = true
    encrypted             = true
    kms_key_id            = local.kms_key_id
    throughput            = 0
    volume_size           = 1000
    volume_type           = "gp2"
  }
}

module "bfd_pipeline_slis" {
  count = local.create_slis ? 1 : 0

  source          = "./modules/bfd_pipeline_slis"
  account_id      = local.account_id
  aws_kms_key_arn = local.kms_key_id
  aws_kms_key_id  = local.kms_key_id
  etl_bucket_id   = aws_s3_bucket.this.id
}

module "bfd_pipeline_dashboard" {
  count = local.create_dashboard ? 1 : 0

  source = "./modules/bfd_pipeline_dashboard"
}

module "bfd_pipeline_slo_alarms" {
  count = local.create_slo_alarms ? 1 : 0

  source = "./modules/bfd_pipeline_slo_alarms"

  alert_sns_override      = var.alert_sns_override
  alert_ok_sns_override   = var.alert_ok_sns_override
  warning_sns_override    = var.warning_sns_override
  warning_ok_sns_override = var.warning_ok_sns_override
}

module "bfd_pipeline_scheduler" {
  # For now, this module only supports the CCW-variant of the pipeline and so should not be included
  # if the CCW pipeline is disabled
  # TODO: Consider removing when RDA pipeline supports on-demand mechanisms
  count = local.pipeline_variant_configs.ccw.enabled ? 1 : 0

  source = "./modules/bfd_pipeline_scheduler"

  account_id     = local.account_id
  etl_bucket_id  = aws_s3_bucket.this.id
  env_kms_key_id = data.aws_kms_key.cmk.key_id
  ccw_pipeline_asg_details = {
    arn  = aws_autoscaling_group.this["ccw"].arn
    name = aws_autoscaling_group.this["ccw"].name
  }
}
