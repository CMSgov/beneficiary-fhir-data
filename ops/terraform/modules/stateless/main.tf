#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  azs             = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config      = { env = var.env_config.env, tags = var.env_config.tags, vpc_id = data.aws_vpc.main.id, zone_id = data.aws_route53_zone.local_zone.id, azs = local.azs }
  is_prod         = substr(var.env_config.env, 0, 4) == "prod"
  port            = 7443
  cw_period       = 60 # Seconds
  cw_eval_periods = 3

  # Add new peerings here
  vpc_peerings_by_env = {
    test = [
      "bfd-test-vpc-to-bluebutton-dev", "bfd-test-vpc-to-bluebutton-test"
    ],
    prod = [
      "bfd-prod-vpc-to-mct-prod-vpc", "bfd-prod-vpc-to-mct-prod-dr-vpc",
      "bfd-prod-vpc-to-dpc-prod-vpc",
      "bfd-prod-vpc-to-bluebutton-prod",
      "bfd-prod-vpc-to-bcda-prod-vpc",
      "bfd-prod-to-ab2d-prod"
    ],
    prod-sbx = [
      "bfd-prod-sbx-to-bcda-dev", "bfd-prod-sbx-to-bcda-test", "bfd-prod-sbx-to-bcda-sbx", "bfd-prod-sbx-to-bcda-opensbx",
      "bfd-prod-sbx-vpc-to-bluebutton-dev", "bfd-prod-sbx-vpc-to-bluebutton-impl", "bfd-prod-sbx-vpc-to-bluebutton-test",
      "bfd-prod-sbx-vpc-to-dpc-prod-sbx-vpc", "bfd-prod-sbx-vpc-to-dpc-test-vpc", "bfd-prod-sbx-vpc-to-dpc-dev-vpc",
      "bfd-prod-sbx-vpc-to-mct-imp-vpc", "bfd-prod-sbx-vpc-to-mct-test-vpc"
    ]
  }
  vpc_peerings = local.vpc_peerings_by_env[var.env_config.env]
}

# Find resources defined outside this script 
# 

# VPC
#
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpc"]
  }
}

data "aws_vpc" "mgmt" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

# VPC peerings
#
data "aws_vpc_peering_connection" "peers" {
  count = length(local.vpc_peerings)
  tags  = { Name = local.vpc_peerings[count.index] }
}

# DNS
#
data "aws_route53_zone" "local_zone" {
  name         = "bfd-${var.env_config.env}.local"
  private_zone = true
}

# S3 Buckets
#
data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "admin" {
  bucket = "bfd-${var.env_config.env}-admin-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "logs" {
  bucket = "bfd-${var.env_config.env}-logs-${data.aws_caller_identity.current.account_id}"
}

# CloudWatch
#
data "aws_sns_topic" "cloudwatch_alarms" {
  name = "bfd-${var.env_config.env}-cloudwatch-alarms"
}

data "aws_sns_topic" "cloudwatch_ok" {
  name = "bfd-${var.env_config.env}-cloudwatch-ok"
}

# # RDS Replicas
# #
# data "aws_db_instance" "replica" {
#   count                   = 3
#   db_instance_identifier  = "bfd-${var.env_config.env}-replica${count.index+1}"
# }

# # RDS Security Groups
# #
# data "aws_security_group" "db_primary" {
#   filter {
#     name        = "tag:Name"
#     values      = ["bfd-${var.env_config.env}-master-rds"]
#   }
# }

# data "aws_security_group" "db_replicas" {
#   filter {
#     name        = "tag:Name"
#     values      = ["bfd-${var.env_config.env}-rds"]
#   }
# }

# Aurora security group
#
data "aws_security_group" "aurora_cluster" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-aurora-cluster"]
  }
}

# Other Security Groups
#
# Find the security group for the Cisco VPN
#
data "aws_security_group" "vpn" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpn-private"]
  }
}

# Find the tools group
#
data "aws_security_group" "tools" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-enterprise-tools"]
  }
}

# Find the management group
#
data "aws_security_group" "remote" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-remote-management"]
  }
}

# Find ansible vault pw read only policy by hardcoded ARN, no other options for this data source
#
data "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  arn = "arn:aws:iam::577373831711:policy/bfd-ansible-vault-pw-ro-s3"
}

##
# Start to build stuff
##

# IAM roles
# 
# Create one for the FHIR server and one for the ETL
module "fhir_iam" {
  source = "../resources/iam"

  env_config = local.env_config
  name       = "fhir"
}

resource "aws_iam_role_policy_attachment" "fhir_iam_ansible_vault_pw_ro_s3" {
  role       = module.fhir_iam.role
  policy_arn = data.aws_iam_policy.ansible_vault_pw_ro_s3.arn
}

# NLB for the FHIR server (SSL terminated by the FHIR server)
#
module "fhir_lb" {
  source = "../resources/lb"

  env_config = local.env_config
  role       = "fhir"
  layer      = "dmz"
  log_bucket = data.aws_s3_bucket.logs.id
  is_public  = var.is_public

  ingress = var.is_public ? {
    description = "Public Internet access"
    port        = 443
    cidr_blocks = ["0.0.0.0/0"]
    } : {
    description = "From VPC peerings, the MGMT VPC, and self"
    port        = 443
    cidr_blocks = concat(data.aws_vpc_peering_connection.peers[*].peer_cidr_block, [data.aws_vpc.mgmt.cidr_block, data.aws_vpc.main.cidr_block])
  }

  egress = {
    description = "To VPC instances"
    port        = local.port
    cidr_blocks = [data.aws_vpc.main.cidr_block]
  }
}

module "lb_alarms" {
  source = "../resources/lb_alarms"

  load_balancer_name     = module.fhir_lb.name
  alarm_notification_arn = data.aws_sns_topic.cloudwatch_alarms.arn
  ok_notification_arn    = data.aws_sns_topic.cloudwatch_ok.arn
  env                    = var.env_config.env
  app                    = "bfd"

  # NLBs only have this metric to alarm on
  healthy_hosts = {
    eval_periods = local.cw_eval_periods
    period       = local.cw_period
    threshold    = 1 # Count
  }
}


# Autoscale group for the FHIR server
#
module "fhir_asg" {
  source = "../resources/asg"

  env_config = local.env_config
  role       = "fhir"
  layer      = "app"
  lb_config  = module.fhir_lb.lb_config

  # Initial size is one server per AZ
  asg_config = {
    min             = local.is_prod ? 2 * length(local.azs) : length(local.azs)
    max             = 8 * length(local.azs)
    desired         = local.is_prod ? 2 * length(local.azs) : length(local.azs)
    sns_topic_arn   = ""
    instance_warmup = 430
  }

  launch_config = {
    # instance_type must support NVMe EBS volumes: https://github.com/CMSgov/beneficiary-fhir-data/pull/110
    # test == c5.xlarge (4 vCPUs and 8GiB mem)
    # prod and prod-sbx == c5.4xlarge (16 vCPUs and 32GiB mem )
    instance_type = var.env_config.env == "test" ? "c5.xlarge" : "c5.4xlarge"
    volume_size   = 60                                                             # GB
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

# FHIR server metrics, per partner
#
module "bfd_server_metrics_all" {
  source = "../resources/bfd_server_metrics"

  env = var.env_config.env

  metric_config = {
    partner_name  = "all"
    partner_regex = "*"
  }
}

module "bfd_server_metrics_bb" {
  source = "../resources/bfd_server_metrics"

  env = var.env_config.env

  metric_config = {
    partner_name  = "bb"
    partner_regex = "*BlueButton*"
  }
}

module "bfd_server_metrics_bcda" {
  source = "../resources/bfd_server_metrics"

  env = var.env_config.env

  metric_config = {
    partner_name  = "bcda"
    partner_regex = "*bcda*"
  }
}

module "bfd_server_metrics_mct" {
  source = "../resources/bfd_server_metrics"

  env = var.env_config.env

  metric_config = {
    partner_name  = "mct"
    partner_regex = "*mct*"
  }
}

module "bfd_server_metrics_dpc" {
  source = "../resources/bfd_server_metrics"

  env = var.env_config.env

  metric_config = {
    partner_name  = "dpc"
    partner_regex = "*dpc*"
  }
}

module "bfd_server_metrics_ab2d" {
  source = "../resources/bfd_server_metrics"

  env = var.env_config.env

  metric_config = {
    partner_name  = "ab2d"
    partner_regex = "*ab2d*"
  }
}

# FHIR server alarms, partner specific
#

# TODO: Deprecate this alarm in favor of metric math expression to more accurately
# represet our error budget
#
module "bfd_server_alarm_all_500s" {
  source = "../resources/bfd_server_alarm"

  env = var.env_config.env

  alarm_config = {
    alarm_name       = "all-500s"
    partner_name     = "all"
    metric_prefix    = "http-requests/count-500"
    eval_periods     = "15"
    period           = "60"
    datapoints       = "15"
    statistic        = "Sum"
    ext_statistic    = null
    threshold        = "8.0"
    alarm_notify_arn = data.aws_sns_topic.cloudwatch_alarms.arn
    ok_notify_arn    = data.aws_sns_topic.cloudwatch_ok.arn
  }
}

module "bfd_server_alarm_all_eob_6s-p95" {
  source = "../resources/bfd_server_alarm"

  env = var.env_config.env

  alarm_config = {
    alarm_name       = "all-eob-6s-p95"
    partner_name     = "all"
    metric_prefix    = "http-requests/latency/eobAll"
    eval_periods     = "15"
    period           = "60"
    datapoints       = "15"
    statistic        = null
    ext_statistic    = "p95"
    threshold        = "6000.0"
    alarm_notify_arn = data.aws_sns_topic.cloudwatch_alarms.arn
    ok_notify_arn    = data.aws_sns_topic.cloudwatch_ok.arn
  }
}

module "bfd_server_alarm_mct_eob_3s_p95" {
  source = "../resources/bfd_server_alarm"

  env = var.env_config.env

  alarm_config = {
    alarm_name       = "mct-eob-3s-p95"
    partner_name     = "mct"
    metric_prefix    = "http-requests/latency/eobAll"
    eval_periods     = "15"
    period           = "60"
    datapoints       = "15"
    statistic        = null
    ext_statistic    = "p95"
    threshold        = "3000.0"
    alarm_notify_arn = data.aws_sns_topic.cloudwatch_alarms.arn
    ok_notify_arn    = data.aws_sns_topic.cloudwatch_ok.arn
  }
}

# ETL server
#
module "bfd_pipeline" {
  source = "../resources/bfd_pipeline"

  env_config = local.env_config
  az         = "us-east-1b" # Same as the master db

  launch_config = {
    ami_id       = var.etl_ami
    account_id   = data.aws_caller_identity.current.account_id
    ssh_key_name = var.ssh_key_name
    git_branch   = var.git_branch_name
    git_commit   = var.git_commit_id
  }

  db_config = {
    db_sg = data.aws_security_group.aurora_cluster.id
  }

  mgmt_config = {
    vpn_sg    = data.aws_security_group.vpn.id
    tool_sg   = data.aws_security_group.tools.id
    remote_sg = data.aws_security_group.remote.id
    ci_cidrs  = [data.aws_vpc.mgmt.cidr_block]
  }

  alarm_notification_arn = data.aws_sns_topic.cloudwatch_alarms.arn
  ok_notification_arn    = data.aws_sns_topic.cloudwatch_ok.arn

  mpm_rda_cidr_block = var.mpm_rda_vpc_config.cidr_block
}
