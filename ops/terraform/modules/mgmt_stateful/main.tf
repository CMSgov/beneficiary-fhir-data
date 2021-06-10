#
# Build the stateful resources for an environment.
#
# This script also builds the associated KMS needed by the stateful and stateless resources
#
locals {
  env_config = { env = var.env_config.env, tags = var.env_config.tags, vpc_id = data.aws_vpc.main.id, zone_id = aws_route53_zone.local_zone.id, azs = var.env_config.azs }
}

# vpc
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

# subnets for app
data "aws_subnet_ids" "app_subnets" {
  vpc_id = data.aws_vpc.main.id

  tags = {
    Layer = "app"
  }
}

# kms
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env_config.env}-cmk"
}

# vpn
data "aws_security_group" "vpn" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpn-private"]
  }
}

# management security group
data "aws_security_group" "tools" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-enterprise-tools"]
  }
}

# tools security group 
data "aws_security_group" "management" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-remote-management"]
  }
}


## VPC Private Local Zone for CNAME Records
#
resource "aws_route53_zone" "local_zone" {
  name = "bfd-${var.env_config.env}.local"
  vpc {
    vpc_id = data.aws_vpc.main.id
  }
}


## IAM policy to allow read access to ansible vault password
#
resource "aws_iam_policy" "ansible_vault_pw_ro_s3" {
  name        = "bfd-${var.env_config.env}-ansible-vault-pw-ro-s3"
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


## S3 Admin Bucket
#
module "admin" {
  source     = "../resources/s3"
  role       = "admin"
  env_config = local.env_config
  kms_key_id = data.aws_kms_key.master_key.arn
  log_bucket = module.logs.id
}


## S3 bucket for logs 
#
module "logs" {
  source     = "../resources/s3"
  role       = "logs"
  env_config = local.env_config
  acl        = "log-delivery-write" # For AWS bucket logs
  kms_key_id = null                 # Use AWS encryption to support AWS Agents writing to this bucket
}


## Jenkins EFS Resources, Mounts, and Security Groups
#
module "efs" {
  source     = "../resources/efs"
  env_config = local.env_config
  role       = "jenkins"
  layer      = "app"
}

resource "aws_ebs_volume" "jenkins_data" {
  availability_zone = local.env_config.azs
  size              = 1000
  type              = "gp2"
  encrypted         = true
  kms_key_id        = data.aws_kms_key.master_key.arn

  tags = {
    Name       = "bfd-${var.env_config.env}-jenkins-data"
    cpm_backup = "4HR Daily Weekly Monthly"
  }
}


## IAM Roles, Profiles and Policies for Packer
#
resource "aws_iam_role" "packer" {
  name = "bfd-${var.env_config.env}-packer"

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
  name = "bfd-${var.env_config.env}-packer"
  role = aws_iam_role.packer.name
}

resource "aws_iam_policy" "packer_s3" {
  name        = "bfd-${var.env_config.env}-packer-s3"
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
                "arn:aws:s3:::${var.bfd_packages_bucket}/*",
                "arn:aws:s3:::${var.bfd_packages_bucket}"
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

resource "aws_iam_role_policy_attachment" "packer_EFS" {
  role       = aws_iam_role.packer.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonElasticFileSystemFullAccess"
}


## GitHub Actions IAM Roles and Policies
#

# policy to allow gh actions to manage s3 buckets for integration tests
resource "aws_iam_policy" "github_actions_s3its" {
  name        = "bfd-${var.env_config.env}-github-actions-s3its"
  description = "GitHub Actions policy for S3 integration tests"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BFDGitHubActionsS3ITs",
      "Action": [
        "s3:CreateBucket",
        "s3:ListAllMyBuckets"
      ],
      "Effect": "Allow",
      "Resource": "*"
    },
    {
      "Sid": "BFDGitHubActionsS3ITsBucket",
      "Action": [
        "s3:DeleteBucket",
        "s3:HeadBucket",
        "s3:ListBucket"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*"
    },
    {
      "Sid": "BFDGitHubActionsS3ITsObject",
      "Action": [
        "s3:DeleteObject",
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::bb-test-*/*"
    }
  ]
}
EOF
}

resource "aws_iam_user" "github_actions" {
  name = "bfd-${var.env_config.env}-github-actions"
}

resource "aws_iam_user_policy_attachment" "github_actions_s3its" {
  user       = aws_iam_user.github_actions.name
  policy_arn = aws_iam_policy.github_actions_s3its.arn
}
