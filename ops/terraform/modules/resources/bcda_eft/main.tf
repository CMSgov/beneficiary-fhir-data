#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ VARS & DATA SOURCES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#
locals {
  tags              = merge({Layer=var.layer, role=var.role}, var.env_config.tags)
  posix_user_id     = "1500"
  posix_group_id    = "1500"
  bcda_root_dir     = "/dropbox"
}

# returns selected vpc (bfd-prod-vpc, bfd-prod-sbx-vpc, etc)
data "aws_vpc" "main" {
  filter {
    name    = "tag:Name"
    values  = ["bfd-${var.env_config.env}-vpc"]
  }
}

# used to get our current account number
data "aws_caller_identity" "current" {}

# returns all "data" subnet id's available to the seleted vpc
data "aws_subnet_ids" "etl" {
  vpc_id    = var.env_config.vpc_id
  
  filter {
    name    = "tag:Name"
    values  = ["bfd-${var.env_config.env}-az*-data"]
  }
}

# returns all "data" subnets (used for grabbing cidr_blocks)
data "aws_subnet" "etl" {
  vpc_id    = var.env_config.vpc_id
  for_each  = toset(data.aws_subnet_ids.etl.ids)
  id        = each.value

  filter {
    name    = "tag:Name"
    values  = ["bfd-${var.env_config.env}-az*-data"]
  }
}

# targets the bcda eft efs cmk (encryption key)
# using this in case we move the KMS resources defined in this file somewhere else in the future
# data "aws_kms_key" "bcda_eft_efs" {
#   key_id = "alias/bcda-eft-efs-${var.env_config.env}-cmk"
# }

# etl instance role
data "aws_iam_role" "etl_instance" {
  name = "bfd-${var.env_config.env}-bfd_pipeline-role"
}


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ENCRYPTION KEYS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# provision the cmk TODO: update policy to use roles instead of users
resource "aws_kms_key" "bcda_eft_efs" {
  description = "bcda-eft-efs-${var.env_config.env}-cmk"
  key_usage   = "ENCRYPT_DECRYPT"
  is_enabled  = true
  tags        = merge({Name="bcda-eft-${var.env_config.env}-efs"}, local.tags)

  policy = <<POLICY
{
  "Version" : "2012-10-17",
  "Id" : "bcda-eft-efs-${var.env_config.env}-cmk-policy",
  "Statement" : [ {
    "Sid" : "Allow root full admin",
    "Effect" : "Allow",
    "Principal" : {
      "AWS" : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
    },
    "Action" : "kms:*",
    "Resource" : "*"
  }, {
    "Sid" : "Allow admin users",
    "Effect" : "Allow",
    "Principal" : {
      "AWS" : [
          "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/AJHL",
          "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/D6LU",
          "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/BFD_System_Maintainer",
          "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/VZG9"
      ]
    },
    "Action" : [
        "kms:Create*",
        "kms:Describe*",
        "kms:Enable*",
        "kms:List*",
        "kms:Put*",
        "kms:Update*",
        "kms:Revoke*",
        "kms:Disable*",
        "kms:Get*",
        "kms:Delete*",
        "kms:TagResource",
        "kms:UntagResource",
        "kms:ScheduleKeyDeletion",
        "kms:CancelKeyDeletion"
    ],
    "Resource" : "*"
  }, {
    "Sid" : "Allow use of the key",
    "Effect" : "Allow",
    "Principal" : {
      "AWS" : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/bcda-${var.env_config.env}-eft-efs-rw-access-role"
    },
    "Action" : [ "kms:Encrypt",
        "kms:Decrypt",
        "kms:ReEncrypt*",
        "kms:GenerateDataKey*",
        "kms:DescribeKey"
    ],
    "Resource" : "*"
  }, {
    "Sid" : "Allow attachment of persistent resources",
    "Effect" : "Allow",
    "Principal" : {
      "AWS" : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/bcda-${var.env_config.env}-eft-efs-rw-access-role"
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

# # key alias
# resource "aws_kms_alias" "bcda_eft_efs" {
#     name          = "alias/bcda-eft-efs-${var.env_config.env}-cmk"
#     target_key_id = aws_kms_key.bcda_eft_efs.id
# }


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ FILE SYSTEMS AND ACCESS POINTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# BCDA EFT file system
resource "aws_efs_file_system" "bcda_eft" {
    creation_token  = "bcda-eft-${var.env_config.env}-efs"
    encrypted       = "true"
    kms_key_id      = aws_kms_key.bcda_eft_efs.arn
    tags            = merge({Name="bcda-eft-${var.env_config.env}-efs"}, local.tags)

    // BCDA will be responsible for cleaning up after ingestion, but just in case, we transition
    // files not accessed after 7 days to slower storage to save $
    // TODO: create alert if any files are in IA class
    lifecycle_policy {
      transition_to_ia = "AFTER_7_DAYS"
    }
}

# Access point BCDA will mount
# - will automagically root them into the /dropbox directory
# - will perform all file operations as specific posix user & group (e.g., 1500:1500)
resource "aws_efs_access_point" "bcda_eft" {
  file_system_id  = aws_efs_file_system.bcda_eft.id
  tags            = merge({Name="bcda-eft-${var.env_config.env}-efs-ap"}, local.tags)
  
  posix_user {
    gid = local.posix_group_id
    uid = local.posix_user_id
  }

  root_directory {
    path = local.bcda_root_dir
    
    creation_info {
      owner_gid   = local.posix_group_id
      owner_uid   = local.posix_user_id
      permissions = "0755"
    }
  }
}

# Deploys mount targets in all ETL data subnets
# TODO: only deploys mount targets in *existing* subnets. We will need to extend our data
# subnets into all AZ's to ensure we do not incur cross-AZ data charges
resource "aws_efs_mount_target" "bcda_eft" {
  file_system_id  = aws_efs_file_system.bcda_eft.id
  for_each        = data.aws_subnet_ids.etl.ids
  subnet_id       = each.value
}

# EFS file system policy that
# - allows BFD ETL servers full root access
# - allows BCDA read+write access
# - denies all non-tls enabled connections
resource "aws_efs_file_system_policy" "bcda_eft" {
  file_system_id = aws_efs_file_system.bcda_eft.id

  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Id": "bcda-eft-${var.env_config.env}-efs-policy",
    "Statement": [
        {
            "Sid": "allow-etl-full",
            "Effect": "Allow",
            "Principal": {
                "AWS": "*"
            },
            "Action": [
                "elasticfilesystem:ClientMount",
                "elasticfilesystem:ClientRootAccess",
                "elasticfilesystem:ClientWrite"
            ],
            "Resource": "${aws_efs_file_system.bcda_eft.arn}",
            "Condition": {
                "ArnEquals": {
                    "aws:PrincipalArn": "${data.aws_iam_role.etl_instance.arn}"
                }
            }
        },
        {
            "Sid": "allow-bcda-rw",
            "Effect": "Allow",
            "Principal": {
                "AWS": "*"
            },
            "Action": [
                "elasticfilesystem:ClientMount",
                "elasticfilesystem:ClientWrite"
            ],
            "Resource": "${aws_efs_file_system.bcda_eft.arn}",
            "Condition": {
                "ArnEquals": {
                    "aws:PrincipalArn": "arn:aws:iam::${var.bcda_acct_num}:*"
                }
            }
        },
        {
            "Sid": "deny-no-tls",
            "Effect": "Deny",
            "Principal": {
                "AWS": "*"
            },
            "Action": "*",
            "Resource": "${aws_efs_file_system.bcda_eft.arn}",
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

# Creates an IAM role that BCDA account is allowed to assume. This role, along with the policy
# below, allows BCDA to mount the EFS file system with read and write permissions.
# TODO: verify principal
resource "aws_iam_role" "bcda_rw" {
    name               = "bcda-eft-efs-${var.env_config.env}-rw-access-role"
    path               = "/"
    assume_role_policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::${var.bcda_acct_num}:root"
      },
      "Action": "sts:AssumeRole",
      "Condition": {
      }
    }
  ]
}
POLICY
}

# policy attached to above role that
# - allows BCDA to mount the file system
# - grants BCDA read+write privileges
# - allows BCDA to describe our mount targets
resource "aws_iam_policy" "bcda_ap_access" {
    name        = "bcda-eft-efs-${var.env_config.env}-ap-access-policy"
    path        = "/"
    description = ""
    policy      = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowReadWriteMount",
      "Effect": "Allow",
      "Action": [
        "elasticfilesystem:DescribeMountTargets",
        "elasticfilesystem:ClientWrite",
        "elasticfilesystem:ClientMount"
      ],
      "Resource": [
        "${aws_efs_access_point.bcda_eft.arn}"
      ]
    }
  ]
}
POLICY
}

# attaches the above policy to the role
resource "aws_iam_policy_attachment" "bcda_ap_access" {
    name       = "bcda-eft-efs-${var.env_config.env}-ap-access-policy-attachment"
    policy_arn = aws_iam_policy.bcda_ap_access.arn
    roles      = [aws_iam_role.bcda_rw.name]
}


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ NETWORK ACL's ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# security group that allows NFS traffic (TCP/2049) from BFD and BCDA subnets
resource "aws_security_group" "bcda_efs_sg" {
    name            = "bcda-eft-efs-${var.env_config.env}-sg"
    description     = "allows nfs to bcda and bfd subnets"
    vpc_id          = data.aws_vpc.main.id
    tags            = local.tags

  // TODO: delete egress unless needed
//    egress {
//        from_port   = 0
//        to_port     = 0
//        protocol    = "-1"
//        cidr_blocks = ["0.0.0.0/0"]
//    }
}

# allow TCP/2049 from BFD ETL data subnets
resource "aws_security_group_rule" "bfd" {
  description       = "Allow NFS"
  type              = "ingress"
  to_port           = "2049"
  from_port         = "2049"
  protocol          = "tcp"
  security_group_id = aws_security_group.bcda_efs_sg.id
  cidr_blocks       = values(data.aws_subnet.etl).*.cidr_block
}

# allow TCP/2049 from specified BCDA subnets
resource "aws_security_group_rule" "bcda" {
  description       = "Allow NFS"
  type              = "ingress"
  to_port           = "2049"
  from_port         = "2049"
  protocol          = "tcp"
  security_group_id = aws_security_group.bcda_efs_sg.id
  cidr_blocks       = var.bcda_subnets[var.env_config.env]
}
