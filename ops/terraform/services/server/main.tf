## 
# Build the stateless resources for an environment (ASG, security groups, etc)

locals {
  env              = terraform.workspace
  established_envs = ["test", "prod-sbx", "prod"]
  azs              = ["us-east-1a", "us-east-1b", "us-east-1c"]
  legacy_service   = "fhir"
  service          = "server"

  default_tags = {
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Environment    = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/${local.service}"
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

  # ephemeral environment determination is based on the existence of the ephemeral_environment_seed
  # in the common hierarchy
  seed_env         = lookup(local.nonsensitive_common_config, "ephemeral_environment_seed", null)
  is_ephemeral_env = local.seed_env == null ? false : true
  is_prod          = local.env == "prod"

  env_config = {
    default_tags = local.default_tags,
    vpc_id       = data.aws_vpc.main.id,
    zone_id      = data.aws_route53_zone.local_zone.id,
    azs          = local.azs
  }
  port            = 7443
  cw_period       = 60 # Seconds
  cw_eval_periods = 3

  # add new peerings here
  vpc_peerings_by_env = {
    test = [
      "bfd-test-vpc-to-bluebutton-test"
    ],
    prod = [
      "bfd-prod-vpc-to-dpc-prod-vpc",
      "bfd-prod-vpc-to-bluebutton-prod",
      "bfd-prod-vpc-to-bcda-prod-vpc",
      "bfd-prod-to-ab2d-prod"
    ],
    prod-sbx = [
      "bfd-prod-sbx-to-ab2d-dev", "bfd-prod-sbx-to-ab2d-impl", "bfd-prod-sbx-to-ab2d-sbx",
      "bfd-prod-sbx-to-bcda-dev", "bfd-prod-sbx-to-bcda-test", "bfd-prod-sbx-to-bcda-sbx", "bfd-prod-sbx-to-bcda-opensbx",
      "bfd-prod-sbx-vpc-to-bluebutton-impl", "bfd-prod-sbx-vpc-to-bluebutton-test",
      "bfd-prod-sbx-vpc-to-dpc-prod-sbx-vpc", "bfd-prod-sbx-vpc-to-dpc-test-vpc", "bfd-prod-sbx-vpc-to-dpc-dev-vpc"
    ]
  }
  vpc_peerings = local.vpc_peerings_by_env[local.env]
}

## IAM role for FHIR
#
module "fhir_iam" {
  source = "./modules/bfd_server_iam"
}

resource "aws_iam_role_policy_attachment" "fhir_iam_ansible_vault_pw_ro_s3" {
  role       = module.fhir_iam.role
  policy_arn = data.aws_iam_policy.ansible_vault_pw_ro_s3.arn
}


## NLB for the FHIR server (SSL terminated by the FHIR server)
#
module "fhir_lb" {
  source = "./modules/bfd_server_lb"

  role       = local.legacy_service
  layer      = "dmz"
  log_bucket = data.aws_s3_bucket.logs.id
  is_public  = var.is_public

  ingress = var.is_public ? {
    description     = "Public Internet access"
    port            = 443
    cidr_blocks     = ["0.0.0.0/0"]
    prefix_list_ids = []
    } : {
    description     = "From VPN, VPC peerings, the MGMT VPC, and self"
    port            = 443
    cidr_blocks     = concat(data.aws_vpc_peering_connection.peers[*].peer_cidr_block, [data.aws_vpc.mgmt.cidr_block, data.aws_vpc.main.cidr_block])
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.vpn.id, data.aws_ec2_managed_prefix_list.jenkins.id]
  }

  egress = {
    description = "To VPC instances"
    port        = local.port
    cidr_blocks = [data.aws_vpc.main.cidr_block]
  }
}

module "lb_alarms" {
  source = "./modules/bfd_server_lb_alarms"

  load_balancer_name     = module.fhir_lb.name
  alarm_notification_arn = data.aws_sns_topic.cloudwatch_alarms.arn
  ok_notification_arn    = data.aws_sns_topic.cloudwatch_ok.arn
  app                    = "bfd"

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

  env_config = local.env_config
  role       = local.legacy_service
  layer      = "app"
  lb_config  = module.fhir_lb.lb_config

  # Initial size is one server per AZ
  asg_config = {
    min             = local.env == "prod-sbx" ? length(local.azs) : 2 * length(local.azs)
    max             = 8 * length(local.azs)
    max_warm        = 4 * length(local.azs)
    desired         = local.env == "prod-sbx" ? length(local.azs) : 2 * length(local.azs)
    sns_topic_arn   = ""
    instance_warmup = 430
  }

  launch_config = {
    # instance_type must support NVMe EBS volumes: https://github.com/CMSgov/beneficiary-fhir-data/pull/110
    instance_type = "c6i.4xlarge"
    volume_size   = local.env == "prod" ? 250 : 60 # GB
    ami_id        = var.fhir_ami
    key_name      = var.ssh_key_name

    profile       = module.fhir_iam.profile
    user_data_tpl = "fhir_server.tpl" # See templates directory for choices
    account_id    = data.aws_caller_identity.current.account_id
    git_branch    = var.git_branch_name
    git_commit    = var.git_commit_id
  }

  db_config = {
    db_sg = data.aws_security_group.aurora_cluster.id
    role  = "aurora cluster"
  }

  mgmt_config = {
    vpn_sg    = data.aws_security_group.vpn.id
    tool_sg   = data.aws_security_group.tools.id
    remote_sg = data.aws_security_group.remote.id
    ci_cidrs  = [data.aws_vpc.mgmt.cidr_block]
  }
}


## FHIR server metrics, per partner
module "bfd_server_metrics" {
  source = "./modules/bfd_server_metrics"
  env    = local.env
}

module "bfd_server_slo_alarms" {
  source = "./modules/bfd_server_slo_alarms"
  env    = local.env
}

module "bfd_server_log_alarms" {
  source = "./modules/bfd_server_log_alarms"
  env    = local.env
}

## This is where cloudwatch dashboards are managed. 
#
module "bfd_dashboards" {
  source         = "./modules/bfd_server_dashboards"
  dashboard_name = var.dashboard_name
  env            = local.env
}

module "disk_usage_alarms" {
  source = "./modules/bfd_server_disk_alarms"
  env    = local.env
}
