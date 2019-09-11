#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  azs             = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config      = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id, azs=local.azs}
  port            = 7743
  cw_period       = 60    # Seconds
  cw_eval_periods = 3
}

# Find resources defined outside this script 
# 

# VPC
#
data "aws_vpc" "main" {
  filter {
    name = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpc"]
  }
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

data "aws_s3_bucket" "etl" {
  bucket = "bfd-${var.env_config.env}-etl-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "admin" {
  bucket = "bfd-${var.env_config.env}-admin-${data.aws_caller_identity.current.account_id}"
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

# RDS Security Group
#
data "aws_security_group" "db" {
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

# Find the management group
#
data "aws_security_group" "tools" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-enterprise-tools"]
  }
}

# Find the tools group 
#
data "aws_security_group" "remote" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-${var.env_config.env}-remote-management"]
  }
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

module "etl_iam" {
  source = "../resources/iam"

  env_config      = local.env_config
  name            = "etl"
  s3_bucket_arns  = [data.aws_s3_bucket.etl.arn]
}


# NLB for the FHIR server (SSL terminated by the FHIR server)
#
module "fhir_lb" {
  source = "../resources/lb"
  load_balancer_type = "network"

  env_config      = local.env_config
  role            = "fhir"
  layer           = "dmz"
  log_bucket      = data.aws_s3_bucket.admin.id
  ingress_port    = 443
  egress_port     = local.port
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

  # TODO: Dummy values to get started
  launch_config   = {

    instance_type = "m5.2xlarge" 
    volume_size   = 100 # GB
    ami_id        = var.fhir_ami 
    key_name      = var.ssh_key_name 

    profile       = module.fhir_iam.profile
    user_data_tpl = "fhir_server.tpl"       # See templates directory for choices
  }

  db_config       = {
    db_sg         = data.aws_security_group.db.id
    role          = "replica"
  }

  mgmt_config     = {
    vpn_sg        = data.aws_security_group.vpn.id
    tool_sg       = data.aws_security_group.tools.id
    remote_sg     = data.aws_security_group.remote.id
    ci_cidrs      = ["10.252.40.0/21"]
  }
}

# ETL server
#
module "etl_instance" {
  source = "../resources/ec2"

  env_config      = local.env_config
  role            = "etl"
  layer           = "data"
  az              = "us-east-1b" # Same as the master db

  # TODO: Dummy values to get started
  launch_config   = {

    instance_type = "m5.2xlarge"
    volume_size   = 100 # GB 
    ami_id        = var.etl_ami 

    key_name      = var.ssh_key_name 
    profile       = module.etl_iam.profile
    user_data_tpl = "default.tpl"
  }

  mgmt_config     = {
    vpn_sg        = data.aws_security_group.vpn.id
    tool_sg       = data.aws_security_group.tools.id
    remote_sg     = data.aws_security_group.remote.id
    ci_cidrs      = ["10.252.40.0/21"]
  }
}
