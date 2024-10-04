module "terraservice" {
  source = "../_modules/bfd-terraservice"

  environment_name     = terraform.workspace
  relative_module_root = "ops/terraform/services/server"
}

locals {
  default_tags          = module.terraservice.default_tags
  env                   = module.terraservice.env
  seed_env              = module.terraservice.seed_env
  is_ephemeral_env      = module.terraservice.is_ephemeral_env
  latest_bfd_release    = module.terraservice.latest_bfd_release
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"
  db_environment        = var.db_environment_override != null ? var.db_environment_override : local.env
  azs                   = ["us-east-1a", "us-east-1b", "us-east-1c"]
  legacy_service        = "fhir"
  service               = "server"

  # NOTE: nonsensitive service-oriented and common config
  nonsensitive_common_map = zipmap(
    data.aws_ssm_parameters_by_path.nonsensitive_common.names,
    nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values)
  )
  nonsensitive_common_config = {
    for key, value in local.nonsensitive_common_map
    : split("/", key)[5] => value
  }
  nonsensitive_service_map = zipmap(
    data.aws_ssm_parameters_by_path.nonsensitive_service.names,
    nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_service.values)
  )
  nonsensitive_service_config = {
    for key, value in local.nonsensitive_service_map
    : split("/", key)[5] => value
  }

  enterprise_tools_security_group = local.nonsensitive_common_config["enterprise_tools_security_group"]
  management_security_group       = local.nonsensitive_common_config["management_security_group"]
  vpn_security_group              = local.nonsensitive_common_config["vpn_security_group"]
  kms_key_alias                   = local.nonsensitive_common_config["kms_key_alias"]
  kms_config_key_alias            = local.nonsensitive_common_config["kms_config_key_alias"]
  ssh_key_pair                    = local.nonsensitive_common_config["key_pair"]
  vpc_name                        = local.nonsensitive_common_config["vpc_name"]

  lb_is_public    = local.nonsensitive_service_config["lb_is_public"]
  lb_ingress_port = local.nonsensitive_service_config["lb_ingress_port"]
  lb_egress_port  = local.nonsensitive_service_config["lb_egress_port"]
  lb_vpc_peerings = jsondecode(local.nonsensitive_service_config["lb_vpc_peerings_json"])

  asg_min_instance_count      = local.nonsensitive_service_config["asg_min_instance_count"]
  asg_max_instance_count      = local.nonsensitive_service_config["asg_max_instance_count"]
  asg_max_warm_instance_count = local.nonsensitive_service_config["asg_max_warm_instance_count"]
  asg_desired_instance_count  = local.nonsensitive_service_config["asg_desired_instance_count"]
  asg_instance_warmup_time    = local.nonsensitive_service_config["asg_instance_warmup_time"]

  launch_template_instance_type     = local.nonsensitive_service_config["launch_template_instance_type"]
  launch_template_volume_iops       = local.nonsensitive_service_config["launch_template_volume_iops"]
  launch_template_volume_size_gb    = local.nonsensitive_service_config["launch_template_volume_size_gb"]
  launch_template_volume_throughput = local.nonsensitive_service_config["launch_template_volume_throughput"]
  launch_template_volume_type       = local.nonsensitive_service_config["launch_template_volume_type"]

  env_config = {
    default_tags = local.default_tags,
    vpc_id       = data.aws_vpc.main.id,
    azs          = local.azs
  }
  cw_period       = 60 # Seconds
  cw_eval_periods = 3

  ami_id = "ami-032e2a66394f48f92" # "ami-087cf32e7146fceec"  # data.aws_ami.main.image_id #  

  create_server_lb_alarms    = !local.is_ephemeral_env || var.force_create_server_lb_alarms
  create_server_metrics      = !local.is_ephemeral_env || var.force_create_server_metrics
  create_server_slo_alarms   = (local.create_server_metrics && !local.is_ephemeral_env) || var.force_create_server_slo_alarms
  create_server_log_alarms   = !local.is_ephemeral_env || var.force_create_server_log_alarms
  create_server_dashboards   = (local.create_server_metrics && !local.is_ephemeral_env) || var.force_create_server_dashboards
  create_server_disk_alarms  = !local.is_ephemeral_env || var.force_create_server_disk_alarms
  create_server_error_alerts = !local.is_ephemeral_env || var.force_create_server_error_alerts
}

## IAM role for FHIR
#
module "fhir_iam" {
  source = "./modules/bfd_server_iam"

  kms_key_alias        = local.kms_key_alias
  kms_config_key_alias = local.kms_config_key_alias
  service              = local.service
  legacy_service       = local.legacy_service
}

## NLB for the FHIR server (SSL terminated by the FHIR server)
#
module "fhir_lb" {
  source = "./modules/bfd_server_lb"

  env_config = local.env_config
  role       = local.legacy_service
  layer      = "dmz"
  log_bucket = data.aws_s3_bucket.logs.id
  is_public  = local.lb_is_public

  ingress = local.lb_is_public ? {
    description     = "Public Internet access"
    port            = local.lb_ingress_port
    cidr_blocks     = ["0.0.0.0/0"]
    prefix_list_ids = []
    } : {
    description     = "From VPN, VPC peerings, the MGMT VPC, and self"
    port            = local.lb_ingress_port
    cidr_blocks     = concat(data.aws_vpc_peering_connection.peers[*].peer_cidr_block, [data.aws_vpc.mgmt.cidr_block, data.aws_vpc.main.cidr_block])
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.vpn.id, data.aws_ec2_managed_prefix_list.jenkins.id]
  }

  egress = {
    description = "To VPC instances"
    port        = local.lb_egress_port
    cidr_blocks = [data.aws_vpc.main.cidr_block]
  }
}

module "lb_alarms" {
  count = local.create_server_lb_alarms ? 1 : 0

  source = "./modules/bfd_server_lb_alarms"

  load_balancer_name = module.fhir_lb.name
  app                = "bfd"

  # NLBs only have this metric to alarm on
  healthy_hosts = {
    eval_periods = local.cw_eval_periods
    period       = local.cw_period
    threshold    = 1 # Count
  }
}


## Autoscale group for the FHIR server
#
module "fhir_asg" {
  source = "./modules/bfd_server_asg"

  kms_key_alias  = local.kms_key_alias
  env_config     = local.env_config
  role           = local.legacy_service
  layer          = "app"
  lb_config      = module.fhir_lb.lb_config
  seed_env       = local.seed_env
  db_environment = local.db_environment

  # Initial size is one server per AZ
  asg_config = {
    min             = local.asg_min_instance_count
    max             = local.asg_max_instance_count
    max_warm        = local.asg_max_warm_instance_count
    desired         = (max(var.override_asg_desired_instance_factor, 1) * local.asg_desired_instance_count)
    sns_topic_arn   = ""
    instance_warmup = local.asg_instance_warmup_time
  }

  # BFD-2588
  disable_asg_autoscale_alarms = var.disable_asg_autoscale_alarms

  launch_config = {
    # instance_type must support NVMe EBS volumes: https://github.com/CMSgov/beneficiary-fhir-data/pull/110
    instance_type     = local.launch_template_instance_type
    volume_size       = local.launch_template_volume_size_gb
    volume_type       = local.launch_template_volume_type
    volume_iops       = local.launch_template_volume_iops
    volume_throughput = local.launch_template_volume_throughput
    ami_id            = local.ami_id
    key_name          = local.ssh_key_pair

    profile       = module.fhir_iam.profile
    user_data_tpl = "fhir_server.tpl" # See templates directory for choices
    account_id    = data.aws_caller_identity.current.account_id
  }

  db_config = {
    db_sg                 = data.aws_security_groups.aurora_cluster.ids
    role                  = "aurora cluster"
    db_cluster_identifier = local.db_cluster_identifier
  }

  mgmt_config = {
    vpn_sg    = data.aws_security_group.vpn.id
    tool_sg   = data.aws_security_group.tools.id
    remote_sg = data.aws_security_group.remote.id
    ci_cidrs  = [data.aws_vpc.mgmt.cidr_block]
  }
}

## FHIR server logs
module "bfd_server_logs" {
  source = "./modules/bfd_server_logs"

  env           = local.env
  kms_key_alias = local.kms_key_alias
}

## FHIR server metrics, per partner
module "bfd_server_metrics" {
  count = local.create_server_metrics ? 1 : 0

  source = "./modules/bfd_server_metrics"
}

module "bfd_server_slo_alarms" {
  count = local.create_server_slo_alarms ? 1 : 0

  source = "./modules/bfd_server_slo_alarms"
}

module "bfd_server_log_alarms" {
  count = local.create_server_log_alarms ? 1 : 0

  source = "./modules/bfd_server_log_alarms"
}

## This is where cloudwatch dashboards are managed.
#
module "bfd_dashboards" {
  count = local.create_server_dashboards ? 1 : 0

  source = "./modules/bfd_server_dashboards"
}

module "disk_usage_alarms" {
  count = local.create_server_disk_alarms ? 1 : 0

  source = "./modules/bfd_server_disk_alarms"

  asg_name = module.fhir_asg.asg_id
}

module "bfd_server_error_alerts" {
  count = local.create_server_error_alerts ? 1 : 0

  source = "./modules/bfd_server_error_alerts"
}
