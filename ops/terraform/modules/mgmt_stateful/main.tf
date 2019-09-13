#
# Build the stateful resources for an environment.
#
# This script also builds the associated KMS needed by the stateful and stateless resources
#

locals {
  azs = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env_config = {env=var.env_config.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id, zone_id=aws_route53_zone.local_zone.id }
}

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
  vpc_id = data.aws_vpc.main.id

  tags = {
    Layer = "app"
  }
}

data "aws_subnet" "selected" {
  count = length(data.aws_subnet_ids.app_subnets.ids)
  id    = tolist(data.aws_subnet_ids.app_subnets.ids)[count.index]
}

# KMS 
#
# The customer master key is created outside of this script
#
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env_config.env}-cmk"
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
# Start to build things
#

# DNS
#
# Build a VPC private local zone for CNAME records
#
resource "aws_route53_zone" "local_zone" {
  name    = "bfd-${var.env_config.env}.local"
  vpc {
    vpc_id = data.aws_vpc.main.id
  }
}

# S3 Admin bucket for logs and other adminstrative 
#
module "admin" {
  source              = "../resources/s3"
  role                = "admin"
  env_config          = local.env_config
  kms_key_id          = data.aws_kms_key.master_key.arn
  acl                 = "log-delivery-write"
}

# IAM policy to allow read access to ansible vault password
#
resource "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  name        = "bfd-ansible-vault-pw-ro-s3"
  description = "ansible vault pw read only S3 policy"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AnsibleVaultPwRO",
      "Action": [
        "kms:Decrypt",
        "s3:GetObject"
      ],
      "Effect": "Allow",
      "Resource": [
        "${data.aws_kms_key.master_key.arn}",
        "${module.admin.arn}/ansible/vault.password"
      ]
    }
  ]
}
EOF
}

# S3 bucket for Build Artifacts
#
module "artifacts" {
  source              = "../resources/s3"
  role                = "artifacts"
  env_config          = local.env_config
  kms_key_id          = data.aws_kms_key.master_key.arn
  log_bucket          = module.admin.id
}

# EBS Volume for Jenkins Data

resource "aws_ebs_volume" "jenkins_data" {
  availability_zone = data.aws_subnet.selected[0].availability_zone
  size              = 1000
  type              = "gp2"

  tags = {
    Name       = "bfd-mgmt-jenkins-data-master"
    cpm_backup = "4HR Daily Weekly Monthly"
  }
}

# IAM Roles, Profiles and Policies for Packer

resource "aws_iam_role" "packer" {
  name = "bfd-packer"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

}

resource "aws_iam_instance_profile" "packer" {
  name = "bfd-packer"
  role = aws_iam_role.packer.name
}

resource "aws_iam_policy" "packer_s3" {
  name = "bfd-packer-s3"
  description = "packer S3 Policy"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "BFDProfile",
            "Effect": "Allow",
            "Action": [
                "s3:GetObjectAcl",
                "s3:GetObject",
                "s3:GetObjectVersionAcl",
                "s3:GetObjectTagging",
                "s3:ListBucket",
                "s3:GetObjectVersion"
            ],
            "Resource": [
                "arn:aws:s3:::bfd-packages/*",
                "arn:aws:s3:::bfd-packages"
            ]
        }
    ]
}
EOF

}

resource "aws_iam_role_policy_attachment" "packer_S3" {
  role       = aws_iam_role.packer.name
  policy_arn = aws_iam_policy.packer_s3.arn
}
