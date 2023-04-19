locals {
  account_id        = data.aws_caller_identity.current.account_id
  env               = terraform.workspace
  layer             = "data"
  established_envs  = ["test", "prod-sbx", "prod"]
  create_etl_user   = local.is_prod || var.force_etl_user_creation
  create_dashboard  = contains(local.established_envs, local.env) || var.force_dashboard_creation
  create_slo_alarms = contains(local.established_envs, local.env) || var.force_slo_alarms_creation
  jdbc_suffix       = var.jdbc_suffix

  # NOTE: Some resources use a 'pipeline' name while others use 'etl'. There's no simple solution for renaming all resources.
  # We must tolerate this for now.
  service        = "pipeline"
  legacy_service = "etl"

  default_tags = {
    Environment    = local.env
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

  # ephemeral environment determination is based on the existence of the ephemeral_environment_seed in the common hierarchy
  seed_env         = lookup(local.nonsensitive_common_config, "ephemeral_environment_seed", null)
  is_ephemeral_env = local.seed_env == null ? false : true
  is_prod          = local.env == "prod"

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
  pipeline_instance_configs = {
    rda = {
      enabled       = var.create_rda_pipeline
      instance_name = "rda"
      instance_type = local.nonsensitive_rda_service_config["instance_type"]
      tags = {
        Name = "bfd-${local.env}-${local.service}-rda"
      }
    }
    ccw = {
      enabled       = var.create_ccw_pipeline
      instance_name = "ccw"
      instance_type = local.nonsensitive_ccw_service_config["instance_type"]
      tags = {
        Name = "bfd-${local.env}-${local.service}-ccw"
      }
    }
  }
  pipeline_instances = { for k, v in local.pipeline_instance_configs : k => local.pipeline_instance_configs[k] if local.pipeline_instance_configs[k].enabled }
}

resource "aws_instance" "pipeline" {
  for_each = { for server in local.pipeline_instances : server.instance_name => server }

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
    http_tokens                 = "optional"
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
