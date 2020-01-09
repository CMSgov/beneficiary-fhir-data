#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  env_config = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id, azs=var.az}
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

data "aws_s3_bucket" "artifacts" {
  bucket = "bfd-${var.env_config.env}-artifacts-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "admin" {
  bucket = "bfd-${var.env_config.env}-admin-${data.aws_caller_identity.current.account_id}"
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

// #
// # Start to build stuff
// #
// # LB for the Jenkins Server
// #
module "jenkins_lb" {
  source = "../resources/lb"
  load_balancer_type = "network"
  env_config      = local.env_config
  role            = "jenkins"
  layer           = "app"
  log_bucket      = data.aws_s3_bucket.admin.id
  ingress_port    = 443
  egress_port     = 443
}

module "jenkins" {
  source = "../resources/jenkins"
  env_config            = local.env_config
  vpc_id                = data.aws_vpc.main.id
  vpn_security_group_id = var.vpn_security_group_id
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
    instance_type = var.instance_size
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
