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
  green_id              = "green"
  blue_id               = "blue"
  lb_name               = "bfd-${local.env}-${local.legacy_service}-nlb"
  tg_health_check_config = {
    healthy_threshold             = 3
    health_check_interval_seconds = 10
    health_check_timeout_seconds  = 8
    unhealthy_threshold           = 2
  }

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
  sensitive_service_map = zipmap(
    data.aws_ssm_parameters_by_path.sensitive_service.names,
    nonsensitive(data.aws_ssm_parameters_by_path.sensitive_service.values)
  )
  sensitive_service_config = {
    for key, value in local.sensitive_service_map
    : split("/", key)[5] => value
  }


  enterprise_tools_security_group = local.nonsensitive_common_config["enterprise_tools_security_group"]
  management_security_group       = local.nonsensitive_common_config["management_security_group"]
  vpn_security_group              = local.nonsensitive_common_config["vpn_security_group"]
  kms_key_alias                   = local.nonsensitive_common_config["kms_key_alias"]
  kms_config_key_alias            = local.nonsensitive_common_config["kms_config_key_alias"]
  ssh_key_pair                    = local.nonsensitive_common_config["key_pair"]
  vpc_name                        = local.nonsensitive_common_config["vpc_name"]

  lb_is_public          = tobool(local.nonsensitive_service_config["lb_is_public"])
  lb_blue_ingress_port  = local.nonsensitive_service_config["lb_blue_ingress_port"]
  lb_green_ingress_port = local.nonsensitive_service_config["lb_green_ingress_port"]
  lb_vpc_peerings       = jsondecode(local.nonsensitive_service_config["lb_vpc_peerings_json"])

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

  service_port = local.sensitive_service_config["service_port"]

  env_config = {
    default_tags = local.default_tags,
    vpc_id       = data.aws_vpc.main.id,
    azs          = local.azs
  }
  cw_period       = 60 # Seconds
  cw_eval_periods = 3

  ami_id = data.aws_ami.main.image_id

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

####
/*
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
*/

## Autoscale group for the FHIR server
#
module "fhir_asg" {
  source = "./modules/bfd_server_asg"

  kms_key_alias = local.kms_key_alias
  env_config    = local.env_config
  role          = local.legacy_service
  layer         = "app"
  seed_env      = local.seed_env

  # Initial size is one server per AZ
  asg_config = {
    min             = local.asg_min_instance_count
    max             = local.asg_max_instance_count
    max_warm        = local.asg_max_warm_instance_count
    desired         = local.asg_desired_instance_count
    sns_topic_arn   = ""
    instance_warmup = local.asg_instance_warmup_time
  }

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

  lb_config = {
    name               = "bfd-${local.env}-${local.legacy_service}-nlb"
    internal           = !local.lb_is_public
    load_balancer_type = "network"
    ip_address_type    = "ipv4"
    load_balancer_security_group_config = {
      egress = {
        description = "To VPC instances"
        cidr_blocks = [data.aws_vpc.main.cidr_block]
      }
      ingress = local.lb_is_public ? {
        description     = "Public Internet access"
        cidr_blocks     = ["0.0.0.0/0"]
        prefix_list_ids = []
        } : {
        description     = "From VPN, VPC peerings, the MGMT VPC, and self"
        cidr_blocks     = concat(data.aws_vpc_peering_connection.peers[*].peer_cidr_block, [data.aws_vpc.mgmt.cidr_block, data.aws_vpc.main.cidr_block])
        prefix_list_ids = [data.aws_ec2_managed_prefix_list.vpn.id, data.aws_ec2_managed_prefix_list.jenkins.id]
      }
    }
    load_balancer_listener_config = [
      {
        id                  = local.green_id
        port                = local.lb_green_ingress_port
        protocol            = "TCP"
        default_action_type = "forward"
      },
      {
        id                  = local.blue_id
        port                = local.lb_blue_ingress_port
        protocol            = "TCP"
        default_action_type = "forward"
      }
    ]
    target_group_config = [
      {
        id                            = local.green_id
        name                          = "${local.lb_name}-tg-${local.green_id}"
        port                          = local.service_port
        deregisteration_delay_seconds = 60
        protocol                      = "TCP"
        health_check_config           = local.tg_health_check_config
      },
      {
        id                            = local.blue_id
        name                          = "${local.lb_name}-tg-${local.blue_id}"
        port                          = local.service_port
        deregisteration_delay_seconds = 60
        protocol                      = "TCP"
        health_check_config           = local.tg_health_check_config
      }
    ]
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

  asg_names = module.fhir_asg.asg_ids
}

module "bfd_server_error_alerts" {
  count = local.create_server_error_alerts ? 1 : 0

  source = "./modules/bfd_server_error_alerts"
}
