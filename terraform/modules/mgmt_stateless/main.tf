#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  azs = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id, azs=local.azs}
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

# Subnets
data "aws_subnet_ids" "app_subnets" {
  vpc_id = data.aws_vpc.shared_services.id

  tags = {
    Layer = "app"
  }
}

data "aws_subnet_ids" "dmz_subnets" {
  vpc_id = "${data.aws_vpc.shared_services.id}"

  tags {
    Layer = "dmz"
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

#
# Start to build stuff
#
# LB for the Jenkins Server
#
module "jenkins_lb" {
  source = "../resources/lb"

  env_config      = local.env_config
  role            = "jenkins"
  layer           = "app"
  log_bucket      = data.aws_s3_bucket.admin.id
  ingress_port    = 443
  egress_port     = 80
}
# Jenkins Module (ELB, ASG, EC2, IAM)

module "jenkins" {
  source = "../resources/jenkins"

  vpc_id                = data.aws_vpc.main.id
  app_subnets           = [data.aws_subnet_ids.app_subnets.ids]
  elb_subnets           = [data.aws_subnet_ids.dmz_subnets.ids]
  vpn_security_group_id = var.vpn_security_group_id
  ami_id                = var.jenkins_ami
  key_name              = var.jenkins_key_name
  tls_cert_arn          = var.jenkins_tls_cert_arn
  lb_config             = module.jenkins_lb.lb_config
  
  env_config      = local.env_config
  role            = "jenkins"
  layer           = "app"
  lb_config       = module.jenkins_lb.lb_config

  # Initial size is one server per AZ
  asg_config      = {
    min           = 3/length(local.azs)
    max           = 3/length(local.azs)
    desired       = 3/length(local.azs)
    sns_topic_arn = ""
  }

  # TODO: Dummy values to get started
  launch_config   = {
    instance_type = "m5.xlarge" 
    ami_id        = "ami-08d9f94f51d962ca9" 
    key_name      = "bfd-jenkins"
    profile       = "jenkins_profile"
  }
}
