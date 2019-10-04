#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  azs               = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config        = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id, azs=local.azs}
  port              = 7443
  cw_period         = 60    # Seconds
  cw_eval_periods   = 3

  # Add new peerings here
  vpc_peerings_by_env = {
    test            = [
      "bfd-test-vpc-to-bluebutton-dev", "bfd-test-vpc-to-bluebutton-test"
    ],
    prod            = [
      "bfd-prod-vpc-to-mct-prod-vpc", "bfd-prod-vpc-to-mct-prod-dr-vpc", 
      "bfd-prod-vpc-to-dpc-prod-vpc", 
      "bfd-prod-vpc-to-dpc-prod-vpc", 
      "bfd-prod-vpc-to-bcda-prod-vpc"
    ],
    prod-sbx        = [
      "bfd-prod-sbx-to-bcda-dev", "bfd-prod-sbx-to-bcda-test", "bfd-prod-sbx-to-bcda-sbx", "bfd-prod-sbx-to-bcda-opensbx",
      "bfd-prod-sbx-vpc-to-bluebutton-dev", "bfd-prod-sbx-vpc-to-bluebutton-impl", "bfd-prod-sbx-vpc-to-bluebutton-test",
      "bfd-prod-sbx-vpc-to-dpc-prod-sbx-vpc", "bfd-prod-sbx-vpc-to-dpc-test-vpc",
      "bfd-prod-sbx-vpc-to-mct-imp-vpc", "bfd-prod-sbx-vpc-to-mct-test-vpc"
    ]
  }
  vpc_peerings      = local.vpc_peerings_by_env[var.env_config.env]
}

# Find resources defined outside this script 
# 

# VPC
#
data "aws_vpc" "main" {
  filter {
    name    = "tag:Name"
    values  = ["bfd-${var.env_config.env}-vpc"]
  }
}

data "aws_vpc" "mgmt" {
  filter {
    name    = "tag:Name"
    values  = ["bfd-mgmt-vpc"]
  }
}

# VPC peerings
#
data "aws_vpc_peering_connection" "peers" {
  count     = length(local.vpc_peerings)
  tags      = {Name=local.vpc_peerings[count.index]}
}

# DNS
#
data "aws_route53_zone" "local_zone" {
  name          = "bfd-${var.env_config.env}.local"
  private_zone  = true
}

# S3 Buckets
#
data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "etl" {
  bucket = "bfd-${var.env_config.env}-etl-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "admin" {
  bucket = "bfd-${var.env_config.env}-admin-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "logs" {
  bucket = "bfd-${var.env_config.env}-logs-${data.aws_caller_identity.current.account_id}"
}

# CloudWatch
#
data "aws_sns_topic" "cloudwatch_alarms" {
  name  = "bfd-${var.env_config.env}-cloudwatch-alarms"
}

data "aws_sns_topic" "cloudwatch_ok" {
  name  = "bfd-${var.env_config.env}-cloudwatch-ok"
}

# RDS Replicas
#
data "aws_db_instance" "replica" {
  count                   = 3
  db_instance_identifier  = "bfd-${var.env_config.env}-replica${count.index+1}"
}

# RDS Security Groups
#
data "aws_security_group" "db_primary" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-master-rds"]
  }
}

data "aws_security_group" "db_replicas" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-rds"]
  }
}

# Other Security Groups
#
# Find the security group for the Cisco VPN
#
data "aws_security_group" "vpn" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-vpn-private"]
  }
}

# Find the tools group
#
data "aws_security_group" "tools" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-enterprise-tools"]
  }
}

# Find the management group
#
data "aws_security_group" "remote" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-remote-management"]
  }
}

# Find ansible vault pw read only policy by hardcoded ARN, no other options for this data source
#
data "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  arn           = "arn:aws:iam::577373831711:policy/bfd-ansible-vault-pw-ro-s3"
}

#
# Start to build stuff
#

# IAM roles
# 
# Create one for the FHIR server and one for the ETL
module "fhir_iam" {
  source = "../resources/iam"

  env_config      = local.env_config
  name            = "fhir"
}

resource "aws_iam_role_policy_attachment" "fhir_iam_ansible_vault_pw_ro_s3" {
  role            = module.fhir_iam.role
  policy_arn      = data.aws_iam_policy.ansible_vault_pw_ro_s3.arn
}

module "etl_iam" {
  source = "../resources/iam"

  env_config      = local.env_config
  name            = "etl"
  s3_bucket_arns  = [data.aws_s3_bucket.etl.arn]
}

resource "aws_iam_role_policy_attachment" "etl_iam_ansible_vault_pw_ro_s3" {
  role            = module.etl_iam.role
  policy_arn      = data.aws_iam_policy.ansible_vault_pw_ro_s3.arn
}

# NLB for the FHIR server (SSL terminated by the FHIR server)
#
module "fhir_lb" {
  source = "../resources/lb"

  env_config      = local.env_config
  role            = "fhir"
  layer           = "dmz"
  log_bucket      = data.aws_s3_bucket.logs.id

  ingress = {
    description   = "From VPC peerings, the MGMT VPC, and self"
    port          = 443
    cidr_blocks   = concat(data.aws_vpc_peering_connection.peers[*].peer_cidr_block, [data.aws_vpc.mgmt.cidr_block, data.aws_vpc.main.cidr_block])
  }

  egress = {
    description   = "To VPC instances"
    port          = local.port
    cidr_blocks   = [data.aws_vpc.main.cidr_block]
  }    
}

module "lb_alarms" {
  source = "../resources/lb_alarms"  

  load_balancer_name            = module.fhir_lb.name
  alarm_notification_arn        = data.aws_sns_topic.cloudwatch_alarms.arn
  ok_notification_arn           = data.aws_sns_topic.cloudwatch_ok.arn
  env                           = var.env_config.env
  app                           = "bfd"

  # NLBs only have this metric to alarm on
  healthy_hosts   = {
    eval_periods  = local.cw_eval_periods
    period        = local.cw_period
    threshold     = 1     # Count
  }
}


# Autoscale group for the FHIR server
#
module "fhir_asg" {
  source = "../resources/asg"

  env_config      = local.env_config
  role            = "fhir"
  layer           = "app"
  lb_config       = module.fhir_lb.lb_config

  # Initial size is one server per AZ
  asg_config      = {
    min           = length(local.azs)
    max           = 2*length(local.azs)
    desired       = length(local.azs)
    sns_topic_arn = ""
  }

  launch_config   = {
    instance_type   = "m5.2xlarge" 
    volume_size     = 100 # GB
    ami_id          = var.fhir_ami 
    key_name        = var.ssh_key_name 

    profile         = module.fhir_iam.profile
    user_data_tpl   = "fhir_server.tpl"       # See templates directory for choices
    account_id      = data.aws_caller_identity.current.account_id
    git_branch      = var.git_branch_name
    git_commit      = var.git_commit_id
  }

  db_config       = {
    db_sg         = data.aws_security_group.db_replicas.id
    role          = "replica"
  }

  mgmt_config     = {
    vpn_sg        = data.aws_security_group.vpn.id
    tool_sg       = data.aws_security_group.tools.id
    remote_sg     = data.aws_security_group.remote.id
    ci_cidrs      = [data.aws_vpc.mgmt.cidr_block]
  }
}

# ETL server
#
module "bfd_pipeline" {
  source = "../resources/bfd_pipeline"

  env_config      = local.env_config
  az              = "us-east-1b" # Same as the master db

  launch_config   = {
    ami_id        = var.etl_ami
    ssh_key_name  = var.ssh_key_name
    profile       = module.etl_iam.profile
    git_branch    = var.git_branch_name
    git_commit    = var.git_commit_id
  }

  db_config       = {
    db_sg         = data.aws_security_group.db_primary.id
  }

  mgmt_config     = {
    vpn_sg        = data.aws_security_group.vpn.id
    tool_sg       = data.aws_security_group.tools.id
    remote_sg     = data.aws_security_group.remote.id
    ci_cidrs      = [data.aws_vpc.mgmt.cidr_block]
  }
}

# Cloudwatch Log Metric Filters

module "cw_metric_filters" {
  source          = "../resources/cw_metric_filters"
  env             = var.env_config.env
}

# Cloudwatch Log Metric Filter Alarms

module "cw_metric_alarms" {
  source          = "../resources/cw_metric_alarms"
  env                           = var.env_config.env
  app                           = "bfd"
  alarm_notification_arn        = data.aws_sns_topic.cloudwatch_alarms.arn
  ok_notification_arn           = data.aws_sns_topic.cloudwatch_ok.arn

}


