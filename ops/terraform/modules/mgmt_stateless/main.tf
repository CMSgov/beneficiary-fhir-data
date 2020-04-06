#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  env_config      = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id, azs=var.azs}
  port            = 443
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
    values = ["bfd-mgmt-vpc"]
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

data "aws_s3_bucket" "admin" {
  bucket = "bfd-${var.env_config.env}-admin-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "logs" {
  bucket = "bfd-${var.env_config.env}-logs-${data.aws_caller_identity.current.account_id}"
}

# Other Security Groups
#
# Find the security group for the Cisco VPN
#
data "aws_security_group" "vpn" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-mgmt-vpn-private"]
  }
}

# Find the management group
#
data "aws_security_group" "tools" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-mgmt-enterprise-tools"]
  }
}

# Find the tools group 
#
data "aws_security_group" "remote" {
  filter {
    name        = "tag:Name"
    values      = ["bfd-mgmt-remote-management"]
  }
}

// #
// # Start to build stuff
// #
// # LB for the Jenkins Server
// #

module "jenkins_lb" {
  source = "../resources/lb"

  env_config            = local.env_config
  role                  = "jenkins"
  layer                 = "dmz"
  log_bucket            = data.aws_s3_bucket.admin.id
  is_public             = var.is_public

  ingress = var.is_public ? {
    description   = "Public Internet access"
    port          = 443
    cidr_blocks   = ["0.0.0.0/0"]
  } : {
    description   = "From Self, and VPN"
    port          = 443
    cidr_blocks   = concat([data.aws_vpc.main.cidr_block], ["10.0.0.0/8"])
  }

  egress = {
    description   = "To VPC instances"
    port          = local.port
    cidr_blocks   = [data.aws_vpc.main.cidr_block]
  }   
}

module "jenkins" {
  source = "../resources/jenkins"
  env_config            = local.env_config
  vpc_id                = data.aws_vpc.main.id
  vpn_security_group_id = data.aws_security_group.vpn.id
  ami_id                = var.jenkins_ami
  key_name              = var.jenkins_key_name
  layer                 = "app"
  role                  = "jenkins"
  lb_config             = module.jenkins_lb.lb_config
  
  # Initial size is one server per AZ
  asg_config      = {
    min           = 1
    max           = 1
    desired       = 1
    sns_topic_arn = ""
  }

  # TODO: Dummy values to get started
  launch_config   = {
    instance_type = var.jenkins_instance_size
    ami_id        = var.jenkins_ami 
    key_name      = var.jenkins_key_name
    profile       = "bfd-jenkins"
  }
  
  mgmt_config     = {
    vpn_sg        = data.aws_security_group.vpn.id
    tool_sg       = data.aws_security_group.tools.id
    remote_sg     = data.aws_security_group.remote.id
    ci_cidrs      = [var.mgmt_network_ci_cidrs]
  }
}