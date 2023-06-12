locals {
  account_id     = data.aws_caller_identity.current.account_id
  env            = terraform.workspace
  kms_key_alias  = "alias/bfd-${local.env}-cmk"
  kms_key_admins = sort([for user in values(data.aws_iam_user.kms_key_admins) : user.arn])

  default_tags = {
    Environment    = local.env
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/env/global/kms"
  }
}

data "aws_caller_identity" "current" {}

# TODO: populating this SSM value isn't defined anywhere yet.
data "aws_ssm_parameter" "kms_key_admins" {
  name = "/bfd/global/terraform/sensitive/kms_key_admins"
}

# TODO: this may fail on subsequent versions of terraform with sensitive value incompatibility with for_each statements
#       and might require wrapping in a call to `nonsensitive()`
data "aws_iam_user" "kms_key_admins" {
  for_each  = toset(split(" ", data.aws_ssm_parameter.kms_key_admins.value))
  user_name = each.value
}

resource "aws_kms_key" "this" {
  enable_key_rotation                = true
  bypass_policy_lockout_safety_check = false
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Id" : "key-consolepolicy-3",
      "Statement" : [
        {
          "Sid" : "Enable IAM User Permissions",
          "Effect" : "Allow",
          "Principal" : {
            "AWS" : "arn:aws:iam::${local.account_id}:root"
          },
          "Action" : "kms:*",
          "Resource" : "*"
        },
        {
          "Sid" : "Allow access for Key Administrators",
          "Effect" : "Allow",
          "Principal" : {
            "AWS" : local.kms_key_admins
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
            "kms:TagResource",
            "kms:UntagResource",
            "kms:ScheduleKeyDeletion",
            "kms:CancelKeyDeletion"
          ],
          "Resource" : "*"
        },
        {
          "Sid" : "Allow use of the key",
          "Effect" : "Allow",
          "Principal" : {
            "AWS" : [
              "arn:aws:iam::${local.account_id}:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling"
            ]
          },
          "Action" : [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncryptTo",
            "kms:GenerateDataKey",
            "kms:GenerateDataKeyWithoutPlaintext",
            "kms:DescribeKey"
          ],
          "Resource" : "*"
        },
        {
          "Sid" : "Allow attachment of persistent resources",
          "Effect" : "Allow",
          "Principal" : {
            "AWS" : [
              "arn:aws:iam::${local.account_id}:role/aws-service-role/autoscaling.amazonaws.com/AWSServiceRoleForAutoScaling"
            ]
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
        },
        {
          "Sid" : "Allow CloudWatch to use the key to encrypt log groups",
          "Effect" : "Allow",
          "Principal" : {
            "Service" : "logs.us-east-1.amazonaws.com"
          },
          "Action" : [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt",
            "kms:GenerateDataKey",
            "kms:Describe"
          ],
          "Resource" : "*"
        },
        {
          "Sid" : "Allow_CloudWatch_for_CMK",
          "Effect" : "Allow",
          "Principal" : {
            "Service" : "cloudwatch.amazonaws.com"
          },
          "Action" : [
            "kms:Decrypt",
            "kms:GenerateDataKey*"
          ],
          "Resource" : "*"
        },
        {
            "Sid" : "Allow S3 to work with encrypted queues and topics",
            "Effect" : "Allow",
            "Principal" : {
                "Service" : "s3.amazonaws.com"
            },
            "Action" : [
                "kms:GenerateDataKey",
                "kms:Decrypt"
            ],
            "Resource" : "*"
        }
      ]
  })
}

resource "aws_kms_alias" "this" {
  name          = local.kms_key_alias
  target_key_id = aws_kms_key.this.arn
}
