#
# Build the stateless resources for an environment. This includes the autoscale groups and 
# associated networking.
#

locals {
  azs               = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config        = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=data.aws_route53_zone.local_zone.id, azs=local.azs}
  port              = 7443

  # Add new peerings here
  vpc_peerings_by_env = {
    test            = [
      "bfd-test-vpc-to-bluebutton-dev", "bfd-test-vpc-to-bluebutton-test"
    ],
    prod            = [
      "bfd-prod-vpc-to-mct-prod-vpc", "bfd-prod-vpc-to-mct-prod-dr-vpc", 
      "bfd-prod-vpc-to-dpc-prod-vpc", 
      "bfd-prod-vpc-to-bluebutton-prod", 
      "bfd-prod-vpc-to-bcda-prod-vpc"
    ],
    prod-sbx        = [
      "bfd-prod-sbx-to-bcda-dev", "bfd-prod-sbx-to-bcda-test", "bfd-prod-sbx-to-bcda-sbx", "bfd-prod-sbx-to-bcda-opensbx",
      "bfd-prod-sbx-vpc-to-bluebutton-dev", "bfd-prod-sbx-vpc-to-bluebutton-impl", "bfd-prod-sbx-vpc-to-bluebutton-test",
      "bfd-prod-sbx-vpc-to-dpc-prod-sbx-vpc", "bfd-prod-sbx-vpc-to-dpc-test-vpc", "bfd-prod-sbx-vpc-to-dpc-dev-vpc", 
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

# Testing Aurora Resources
data "aws_s3_bucket" "logs-aurora" {
  bucket = "bfd-${var.env_config.env}-logs-aurora-${data.aws_caller_identity.current.account_id}"
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

# DNS
#
data "aws_route53_zone" "local_zone" {
  name          = "bfd-${var.env_config.env}.local"
  private_zone  = true
}

data "aws_caller_identity" "current" {}

# VPC peerings
#
data "aws_vpc_peering_connection" "peers" {
  count     = length(local.vpc_peerings)
  tags      = {Name=local.vpc_peerings[count.index]}
}

# Find ansible vault pw read only policy by hardcoded ARN, no other options for this data source
#
data "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  arn           = "arn:aws:iam::577373831711:policy/bfd-ansible-vault-pw-ro-s3"
}

# LB for Aurora Testing
module "fhir_lb-aurora" {
  source = "../resources/lb"

  env_config      = local.env_config
  role            = "fhir-aurora"
  layer           = "dmz"
  log_bucket      = data.aws_s3_bucket.logs-aurora.id
  is_public       = var.is_public

  ingress = var.is_public ? {
    description   = "Public Internet access"
    port          = 443
    cidr_blocks   = ["0.0.0.0/0"]
  } : {
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

# Aurora Autoscale group for testing resources
#
module "fhir-aurora_asg" {
  source = "../resources/asg"

  env_config      = local.env_config
  role            = "fhir-aurora"
  layer           = "app"
  lb_config       = module.fhir_lb-aurora.lb_config

  # Initial size is one server per AZ
  asg_config        = {
    min             = length(local.azs)
    max             = 8*length(local.azs)
    desired         = length(local.azs)
    sns_topic_arn   = ""
    instance_warmup = 430
  }

  launch_config   = {
    # instance_type must support NVMe EBS volumes: https://github.com/CMSgov/beneficiary-fhir-data/pull/110
    instance_type   = "m5.xlarge"             # Use reserve instances
    volume_size     = 100 # GB
    ami_id          = var.fhir_ami 
    key_name        = var.ssh_key_name 

    profile         = "bfd-${var.env_config.env}-fhir-profile"
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
module "bfd_pipeline_aurora" {
  source = "../resources/bfd_pipeline_aurora"

  env_config      = local.env_config
  az              = "us-east-1b" # Same as the master db

  launch_config   = {
    ami_id        = var.etl_ami
    account_id    = data.aws_caller_identity.current.account_id
    ssh_key_name  = var.ssh_key_name
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

