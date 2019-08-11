terraform {
  required_version = "~> 0.12"
}

locals {
  azs = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=aws_route53_zone.local_zone.id }
  db_sgs = [
    aws_security_group.db.id,
    data.aws_security_group.vpn.id,
    data.aws_security_group.tools.id,
    data.aws_security_group.management.id
  ]
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
resource "aws_route53_zone" "local_zone" {
  name    = "bfd-${var.env_config.env}.local"
  vpc {
    vpc_id = data.aws_vpc.main.id
  }
}

# Subnets
# 
data "aws_subnet" "subnets" {
  count     = 3 
  vpc_id    = data.aws_vpc.main.id
  filter {
    name    = "tag:Name"
    values  = ["bfd-${var.env_config.env}-az${count.index+1}-data" ] 
  }
}

# Subnet Group
#
resource "aws_db_subnet_group" "db" {
  name            = "bfd-${local.env_config.env}-subnet-group"
  tags            = local.env_config.tags
  subnet_ids      = [for s in data.aws_subnet.subnets: s.id]
}

# DB Security group
#
# Accept connections from anywhere in the VPC. This choice avoids
# a dependency loop. 
#
# TODO: Use aws_security_group_rule to allow the DB to be created before the web instances. 
#
resource "aws_security_group" "db" {
  name        = "bfd-${var.env_config.env}-rds"
  description = "Security group for DPC DB"
  vpc_id      = local.env_config.vpc_id
  tags        = local.env_config.tags

  ingress {
    from_port = 5432
    protocol  = "tcp"
    to_port   = 5432

    cidr_blocks = ["10.0.0.0/8"]
  }

  egress {
    from_port = 0
    protocol  = "-1"
    to_port   = 0

    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security Groups
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

# Master
#
module "master" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "master"
  availability_zone   = local.azs[0]
  replicate_source_db = ""
  subnet_group        = aws_db_subnet_group.db.name

  vpc_security_group_ids = local.db_sgs
}

# Replicas 
# 
# No count on modules yet
#
module "replica1" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "replica1"
  availability_zone   = local.azs[0]
  replicate_source_db = module.master.identifier
  subnet_group        = aws_db_subnet_group.db.name

  vpc_security_group_ids = local.db_sgs
}

module "replica2" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "replica2"
  availability_zone   = local.azs[1]
  replicate_source_db = module.master.identifier
  subnet_group        = aws_db_subnet_group.db.name

  vpc_security_group_ids = local.db_sgs
}

module "replica3" {
  source              = "../resources/rds"
  db_config           = var.db_config
  env_config          = local.env_config
  role                = "replica3"
  availability_zone   = local.azs[2]
  replicate_source_db = module.master.identifier
  subnet_group        = aws_db_subnet_group.db.name

  vpc_security_group_ids = local.db_sgs
}