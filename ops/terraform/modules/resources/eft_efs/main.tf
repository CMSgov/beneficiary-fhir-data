## VARS & DATA SOURCES
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


## ENCRYPTION KEYS
#

# provision the encryption cmk (keys can be managed by anyone in the kms-key-admins group)
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
  "Statement" : [
    {
      "Sid" : "AllowRootFullAdmin",
      "Effect" : "Allow",
      "Principal" : {
        "AWS" : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
      },
      "Action" : "kms:*",
      "Resource" : "*"
    }
  ]
}
POLICY
}


## FILE SYSTEMS AND ACCESS POINTS
#

# ${var.partner} EFT file system
resource "aws_efs_file_system" "eft" {
  creation_token = "${var.partner}-eft-efs-${var.env_config.env}"
  encrypted      = "true"
  kms_key_id     = aws_kms_key.eft_efs.arn
  tags           = merge({ Name = "${var.partner}-eft-efs-${var.env_config.env}" }, local.tags)

  # ${var.partner} will be responsible for cleaning up after ingestion, but just in case, we transition
  # files not accessed after 7 days to slower storage to save $
  # TODO: create alert if any files are in IA class
  lifecycle_policy {
    transition_to_ia = "AFTER_7_DAYS"
  }
}

# File system Access Point
# - will automagically root NFS client into the ${var.partner_root_dir} directory (e.g., /dropbox)
# - all file actions by the client will automatically be made by var.posix_uid & var.posix_gid (e.g., 1500:1500)
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
      owner_uid   = var.posix_uid
      permissions = "0750"
    }
  }
}

# EFS file system policy that
# - allows BFD ETL servers to mount root r+w
# - allows ${var.partner} to query filesystems via an assumed-role
# - forces TLS (encryption-in-transit)
# - only allows mounting file systems via mount targets
resource "aws_efs_file_system_policy" "eft" {
  file_system_id = aws_efs_file_system.eft.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "${var.partner}-eft-efs-${var.env_config.env}-file-system-policy",
  "Statement": [
    {
      "Sid": "AllowEtlInstanceProfileToMountRootReadWrite",
      "Effect": "Allow",
      "Principal": {
        "AWS": "${data.aws_iam_role.etl_instance.arn}"
      },
      "Action": [
        "elasticfilesystem:ClientMount",
        "elasticfilesystem:ClientWrite",
        "elasticfilesystem:ClientRootAccess",
        "elasticfilesystem:DescribeFileSystem"
      ],
      "Resource": "${aws_efs_file_system.eft.arn}"
    }, {
      "Sid": "AllowPartnerToMountFileSystemReadWriteWhenUsingAP",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::${var.partner_acct_num}:root"
      },
      "Action": [
        "elasticfilesystem:ClientMount",
        "elasticfilesystem:ClientWrite"
      ],
      "Resource": "${aws_efs_file_system.eft.arn}",
      "Condition": {
        "StringEquals": {
          "elasticfilesystem:AccessPointArn": "${aws_efs_access_point.eft.arn}"
        }
      }
    }, {
      "Sid": "DenyAnyConnectionNotUsingTLS",
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
    }, {
      "Sid": "DenyAnyConnectionNotUsingMountTargets",
      "Effect": "Deny",
      "Principal": {
        "AWS": "*"
      },
      "Action": "*",
      "Resource": "${aws_efs_file_system.eft.arn}",
      "Condition": {
        "Bool": {
          "elasticfilesystem:AccessedViaMountTarget": "false"
        }
      }
    }
  ]
}
POLICY
}

# IAM role for querying file systems and mount targets
# ${var.partner} will assume this role
resource "aws_iam_role" "eft_efs_query" {
  name               = "${var.partner}-eft-efs-${var.env_config.env}-query"
  description        = "IAM role to allow ${var.partner} to query BFD (EFS) file systems and mount targets"
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


# policy for the above role
# allows ${var.partner} to query file sytems and mount targets
resource "aws_iam_policy" "eft_efs_query" {
  name        = "${var.partner}-eft-efs-${var.env_config.env}-query"
  path        = "/"
  description = "Assumed role policy to allow ${var.partner} to query BFD file sytems and mount targets"
  policy      = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowRoleToQueryBfdFileSystems",
      "Effect": "Allow",
      "Action": "elasticfilesystem:DescribeFileSystems",
      "Resource": "arn:aws:elasticfilesystem:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:file-system/*"
    },
    {
      "Sid": "AllowRoleToQueryMountTargetsAndAccessPoints",
      "Effect": "Allow",
      "Action": [
        "elasticfilesystem:DescribeMountTargets",
        "elasticfilesystem:DescribeAccessPoints"
      ],
      "Resource": "${aws_efs_file_system.eft.arn}"
    }
  ]
}
POLICY
}

# attaches the query policy to the query role
resource "aws_iam_policy_attachment" "eft_efs_query" {
  name       = "${var.partner}-eft-efs-${var.env_config.env}-query-role-policy-attachment"
  policy_arn = aws_iam_policy.eft_efs_query.arn
  roles      = [aws_iam_role.eft_efs_query.name]
}


## NETWORK ACL's
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


## MOUNT TARGETS
#

# Deploy mount targets into etl data subnets
resource "aws_efs_mount_target" "eft" {
  file_system_id = aws_efs_file_system.eft.id

  # for each data subnet
  for_each  = data.aws_subnet_ids.etl.ids
  subnet_id = each.value

  # attach our nfs rules to the moutn targets
  security_groups = [aws_security_group.eft_efs_sg.id]
}

## MONITORING/ALERTING
#

# sns topic for cloudwatch
resource "aws_sns_topic" "eft_efs" {
  name = "${var.partner}-eft-efs-${var.env_config.env}-cloudwatch-alarms"
}

# hook up efs alerts
module "cloudwatch_alarms_efs" {
  source = "../efs_alarms"

  app                         = var.partner
  env                         = var.env_config.env
  cloudwatch_notification_arn = aws_sns_topic.eft_efs.arn

  filesystem_id = aws_efs_file_system.eft.id
}
