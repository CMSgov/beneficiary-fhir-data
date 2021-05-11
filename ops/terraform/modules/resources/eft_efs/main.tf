#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ VARS & DATA SOURCES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
locals {
  tags = merge({ Layer = var.layer, role = var.role }, var.env_config.tags)
}

# returns selected vpc (bfd-prod-vpc, bfd-prod-sbx-vpc, etc)
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-vpc"]
  }
}

# used to get our current account number
data "aws_caller_identity" "current" {}

# returns all "data" subnet id's available to the seleted vpc
data "aws_subnet_ids" "etl" {
  vpc_id = var.env_config.vpc_id

  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-az*-data"]
  }
}

# returns all "data" subnets (used for grabbing cidr_blocks)
data "aws_subnet" "etl" {
  vpc_id   = var.env_config.vpc_id
  for_each = toset(data.aws_subnet_ids.etl.ids)
  id       = each.value

  filter {
    name   = "tag:Name"
    values = ["bfd-${var.env_config.env}-az*-data"]
  }
}

# etl instance role
data "aws_iam_role" "etl_instance" {
  name = "bfd-${var.env_config.env}-bfd_pipeline-role"
}

# current region
data "aws_region" "current" {}

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ENCRYPTION KEYS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# provision the encryption cmk (keys are managed by the kms-key-admins group)
resource "aws_kms_key" "eft_efs" {
  description         = "${var.partner}-eft-efs-${var.env_config.env}-cmk"
  key_usage           = "ENCRYPT_DECRYPT"
  enable_key_rotation = true
  is_enabled          = true
  tags                = merge({ Name = "${var.partner}-eft-efs-${var.env_config.env}" }, local.tags)

  policy = <<POLICY
{
  "Version" : "2012-10-17",
  "Id" : "${var.partner}-eft-efs-${var.env_config.env}-cmk-policy",
  "Statement" : [ {
    "Sid" : "AllowRootFullAdmin",
    "Effect" : "Allow",
    "Principal" : {
      "AWS" : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
    },
    "Action" : "kms:*",
    "Resource" : "*"
  }, {
    "Sid" : "AllowKeyUse",
    "Effect" : "Allow",
    "Principal" : {
      "AWS" : "${aws_iam_role.eft_efs_rw.arn}"
    },
    "Action" : [ "kms:Encrypt",
        "kms:Decrypt",
        "kms:ReEncrypt*",
        "kms:GenerateDataKey*",
        "kms:DescribeKey"
    ],
    "Resource" : "*"
  }, {
    "Sid" : "AllowEFSKeyUse",
    "Effect" : "Allow",
    "Principal" : {
      "AWS" : "${aws_iam_role.eft_efs_rw.arn}"
    },
    "Action" : [
        "kms:CreateGrant",
        "kms:ListGrants",
        "kms:RevokeGrant"
    ],
    "Resource" : "*",
    "Condition" : {
      "Bool" : {
        "kms:GrantIsForAWSResource" : "true"
      }
    }
  } ]
}
POLICY
}

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ FILE SYSTEMS AND ACCESS POINTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# ${var.partner} EFT file system
resource "aws_efs_file_system" "eft" {
  creation_token = "${var.partner}-eft-efs-${var.env_config.env}"
  encrypted      = "true"
  kms_key_id     = aws_kms_key.eft_efs.arn
  tags           = merge({ Name = "${var.partner}-eft-efs-${var.env_config.env}" }, local.tags)

  // ${var.partner} will be responsible for cleaning up after ingestion, but just in case, we transition
  // files not accessed after 7 days to slower storage to save $
  // TODO: create alert if any files are in IA class
  lifecycle_policy {
    transition_to_ia = "AFTER_7_DAYS"
  }
}

# EFS file system policy that
# - allows BFD ETL servers to mount root r+w
# - allows ${var.partner} to query filesystems via an assumed-role
# - denies all non-tls enabled connections
resource "aws_efs_file_system_policy" "eft" {
  file_system_id = aws_efs_file_system.eft.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "${var.partner}-eft-efs-${var.env_config.env}-file-system-policy",
  "Statement": [
    {
      "Sid": "AllowETLRootRW",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "elasticfilesystem:ClientMount",
        "elasticfilesystem:ClientRootAccess",
        "elasticfilesystem:ClientWrite",
        "elasticfilesystem:DescribeFileSystem"
      ],
      "Resource": "${aws_efs_file_system.eft.arn}",
      "Condition": {
        "ArnEquals": {
          "aws:PrincipalArn": "${data.aws_iam_role.etl_instance.arn}"
        }
      }
    },
    {
      "Sid": "AllowAssumedRoleToQueryFS",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "elasticfilesystem:DescribeFileSystem"
      ],
      "Resource": "${aws_efs_file_system.eft.arn}",
      "Condition": {
        "ArnEquals": {
          "aws:PrincipalArn": "${aws_iam_role.eft_efs_rw.arn}"
        }
      }
    },
    {
      "Sid": "DenyAllIfNotUsingTLS",
      "Effect": "Deny",
      "Principal": {
        "AWS": "*"
      },
      "Action": "*",
      "Resource": "${aws_efs_file_system.eft.arn}",
      "Condition": {
        "Bool": {
          "aws:SecureTransport": "false"
        }
      }
    }
  ]
}
POLICY
}

# Access point ${var.partner} will mount
# - will automagically root them into the /dropbox directory
# - will perform all file operations as specific posix user & group (e.g., 1500:1500)
resource "aws_efs_access_point" "eft" {
  file_system_id = aws_efs_file_system.eft.id
  tags           = merge({ Name = "${var.partner}-eft-efs-${var.env_config.env}-ap" }, local.tags)

  posix_user {
    gid = var.posix_gid
    uid = var.posix_uid
  }

  root_directory {
    path = var.partner_root_dir

    creation_info {
      owner_gid   = var.posix_gid
      owner_uid   = var.posix_gid
      permissions = "0755"
    }
  }
}

# policy to allow querying/mounting access points (via assumed role below)
# - allows ${var.partner} to query file sytems, access points, and mount targets
# - allows ${var.partner} to mount the file system
# - grants ${var.partner} read+write privileges
resource "aws_iam_policy" "eft_efs_ap_access" {
  name        = "${var.partner}-eft-efs-${var.env_config.env}-ap-access-policy"
  path        = "/"
  description = ""
  policy      = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowPartnerToQueryBFDFileSystems",
      "Effect": "Allow",
      "Action": [
        "elasticfilesystem:DescribeFileSystems",
      ],
      "Resource": "arn:aws:elasticfilesystem:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:file-system/*"
    },
    {
      "Sid": "AllowAccessPointMountRW",
      "Effect": "Allow",
      "Action": [
        "elasticfilesystem:DescribeAccessPoints",
        "elasticfilesystem:DescribeMountTargets",
        "elasticfilesystem:DescribeTags",
        "elasticfilesystem:ClientMount",
        "elasticfilesystem:ClientWrite"
      ],
      "Resource": [
        "${aws_efs_access_point.eft.arn}",
        "${aws_efs_file_system.eft.arn}"
      ]
    }
  ]
}
POLICY
}

# IAM role that ${var.partner} is allowed to assume
# TODO: verify principal
resource "aws_iam_role" "eft_efs_rw" {
  name               = "${var.partner}-eft-efs-${var.env_config.env}-rw-access-role"
  path               = "/"
  assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::${var.partner_acct_num}:root"
      },
      "Action": "sts:AssumeRole",
      "Condition": {}
    }
  ]
}
POLICY
}

# attaches the eft_efs_ap_access policy to the eft_efs_rw role
resource "aws_iam_policy_attachment" "eft_efs_ap_access" {
  name       = "${var.partner}-eft-efs-${var.env_config.env}-ap-access-policy-attachment"
  policy_arn = aws_iam_policy.eft_efs_ap_access.arn
  roles      = [aws_iam_role.eft_efs_rw.name]
}


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ NETWORK ACL's ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# security group that allows NFS traffic (TCP/2049) from BFD and ${var.partner} subnets
resource "aws_security_group" "eft_efs_sg" {
  name        = "${var.partner}-eft-efs-${var.env_config.env}-sg"
  description = "allows nfs to ${var.partner} and bfd subnets"
  vpc_id      = data.aws_vpc.main.id
  tags        = local.tags
}

# allow TCP/2049 from BFD ETL data subnets
resource "aws_security_group_rule" "bfd_nfs" {
  description       = "Allow NFS"
  type              = "ingress"
  to_port           = "2049"
  from_port         = "2049"
  protocol          = "tcp"
  security_group_id = aws_security_group.eft_efs_sg.id
  cidr_blocks       = values(data.aws_subnet.etl).*.cidr_block
}

# allow TCP/2049 from specified ${var.partner} subnets
resource "aws_security_group_rule" "partner_nfs" {
  description       = "Allow NFS"
  type              = "ingress"
  to_port           = "2049"
  from_port         = "2049"
  protocol          = "tcp"
  security_group_id = aws_security_group.eft_efs_sg.id
  cidr_blocks       = var.partner_subnets[var.env_config.env]
}

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ MOUNT TARGETS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# Deploy mount targets into etl data subnets
# TODO: only deploys mount targets in *existing* subnets. We will need to extend our data
# TODO: subnets into all AZ's to ensure we do not incur cross-AZ data charges
resource "aws_efs_mount_target" "eft" {
  file_system_id = aws_efs_file_system.eft.id

  # for each data subnet
  for_each  = data.aws_subnet_ids.etl.ids
  subnet_id = each.value

  # attach our nfs rules to the moutn targets
  security_groups = [aws_security_group.eft_efs_sg.id]
}

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ MONITORING/ALERTING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# sns topic for cloudwatch
resource "aws_sns_topic" "cloudwatch_alarms_topic" {
  name = "${var.partner}-eft-efs-${var.env_config.env}-cloudwatch-alarms"
}

# hook up efs alerts
module "cloudwatch_alarms_efs" {
  source = "../eft_efs_alarms"

  app                         = var.partner
  env                         = var.env_config.env
  cloudwatch_notification_arn = aws_sns_topic.cloudwatch_alarms_topic.arn

  filesystem_id = aws_efs_file_system.eft.id
}
