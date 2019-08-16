#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  azs = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id, azs=local.azs}
}

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


# LB for the FHIR server
#
module "fhir_lb" {
  source = "../resources/lb"

  env_config      = local.env_config
  role            = "fhir"
  layer           = "app"
  log_bucket      = data.aws_s3_bucket.admin.id
  ingress_port    = 443
  egress_port     = 7443
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
    instance_type = "m4.large" 
    ami_id        = "ami-0b898040803850657" 
    key_name      = "bfd-rick-test" 
    profile       = module.fhir_iam.profile
  }

  mgmt_config     = {
    vpn_sg        = data.aws_security_group.vpn.id
    tool_sg       = data.aws_security_group.tools.id
    remote_sg     = data.aws_security_group.remote.id
    ci_cidrs      = ["10.252.40.0/21"]
  }
}

