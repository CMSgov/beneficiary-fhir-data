#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  azs = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id }
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
data "aws_s3_bucket" "etl" {
  bucket = "bfd-${var.env_config.env}-etl"
}

data "aws_s3_bucket" "admin" {
  bucket = "bfd-${var.env_config.env}-admin"
}


# Subnets
# 
# Subnets are created by CCS VPC setup
#
data "aws_subnet" "app_subnets" {
  count     = 3 
  vpc_id    = data.aws_vpc.main.id
  filter {
    name    = "tag:Name"
    values  = ["bfd-${var.env_config.env}-az${count.index+1}-app" ] 
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
data "aws_security_group" "management" {
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
module "fhir" {
  source      = "../resources/iam"
  env_config  = var.env_config
  name        = "fhir"
}

module "etl" {
  source          = "../resources/iam"
  env_config      = var.env_config
  name            = "etl"
  s3_bucket_arns  = [data.aws_s3_bucket.etl.arn]
}