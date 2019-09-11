#
# Build a single ec2 instance 
#
locals {
  tags        = merge({Layer=var.layer, role=var.role}, var.env_config.tags)
  is_prod     = substr(var.env_config.env, 0, 4) == "prod" 
}

##
# Data providers
##

# Subnets
# 
# Subnets are created by CCS VPC setup
#
data "aws_subnet" "main" {
  vpc_id            = var.env_config.vpc_id
  availability_zone = var.az
  filter {
    name    = "tag:Layer"
    values  = [var.layer] 
  }
}

# KMS 
#
# The customer master key is created outside of this script
#
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env_config.env}-cmk"
}

data "aws_caller_identity" "current" {}

##
# Resources
##

#
# Security groups
#

# Base security includes management VPC access
#
resource "aws_security_group" "base" {
  name          = "bfd-${var.env_config.env}-${var.role}-base"
  description   = "Allow CI access to app servers"
  vpc_id        = var.env_config.vpc_id
  tags          = merge({Name="bfd-${var.env_config.env}-${var.role}-base"}, local.tags)

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.mgmt_config.ci_cidrs
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    security_groups = [var.mgmt_config.remote_sg,var.mgmt_config.vpn_sg,var.mgmt_config.tool_sg]
  }

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

#
# Build an instance
#
resource "aws_instance" "main" {
  ami                         = var.launch_config.ami_id
  instance_type               = var.launch_config.instance_type
  key_name                    = var.launch_config.key_name
  iam_instance_profile        = var.launch_config.profile

  availability_zone           = var.az
  tags                        = merge({Name="bfd-${var.env_config.env}-${var.role}"}, local.tags)
  volume_tags                 = merge({Name="bfd-${var.env_config.env}-${var.role}", snapshot="true"}, local.tags)
  monitoring                  = true
  associate_public_ip_address = false
  tenancy                     = local.is_prod ? "dedicated" : "default"

  vpc_security_group_ids      = [aws_security_group.base.id]
  subnet_id                   = data.aws_subnet.main.id

  root_block_device {
    volume_type               = "gp2"
    volume_size               = var.launch_config.volume_size
    delete_on_termination     = false
    encrypted                 = true
    kms_key_id                = data.aws_kms_key.master_key.key_id
  }

  user_data                   = templatefile("${path.module}/../templates/${var.launch_config.user_data_tpl}", {
    env    = var.env_config.env,
    accountId = data.aws_caller_identity.current.account_id
  })
}
