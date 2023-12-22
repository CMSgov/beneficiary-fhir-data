resource "aws_iam_role" "logs" {
  name        = "${local.full_name}-logs"
  description = "Role allowing the ${local.full_name}-sftp Transfer Server to write logs"

  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "transfer.amazonaws.com"
          }
        },
      ]
      Version = "2012-10-17"
    }
  )
  force_detach_policies = false
  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AWSTransferLoggingAccess",
  ]
  max_session_duration = 3600
  path                 = "/"
}

resource "aws_iam_role" "eft_user" {
  name        = "${local.full_name}-${local.inbound_sftp_user_username}-sftp-user"
  description = "Role attaching the ${aws_iam_policy.eft_user.name} policy to the ${local.inbound_sftp_user_username} SFTP user"

  assume_role_policy = jsonencode(
    {
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "transfer.amazonaws.com"
          }
        },
      ]
      Version = "2012-10-17"
    }
  )
  managed_policy_arns = [aws_iam_policy.eft_user.arn]

  force_detach_policies = true
}

resource "aws_iam_policy" "eft_user" {
  name = "${local.full_name}-${local.inbound_sftp_user_username}-sftp-user"
  description = join("", [
    "Allows the ${local.inbound_sftp_user_username} SFTP user to access their restricted portion ",
    "of the ${aws_s3_bucket.this.id} S3 bucket when using SFTP"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey",
          ]
          Effect = "Allow"
          Resource = [
            local.kms_key_id
          ]
          Sid = "AllowEncryptionAndDecryptionOfS3Files"
        },
        {
          Sid = "AllowListingOfUserFolder"
          Action = [
            "s3:ListBucket",
            "s3:GetBucketLocation",
          ]
          Effect   = "Allow"
          Resource = [aws_s3_bucket.this.arn]
          Condition = {
            StringLike = {
              "s3:prefix" = [
                "${local.inbound_sftp_s3_home_dir}/*",
                local.inbound_sftp_s3_home_dir
              ]
            }
          }
        },
        {
          Sid    = "HomeDirObjectAccess"
          Effect = "Allow"
          Action = [
            "s3:PutObject",
            "s3:GetObject",
            "s3:DeleteObject",
            "s3:DeleteObjectVersion",
            "s3:GetObjectVersion",
            "s3:GetObjectACL",
            "s3:PutObjectACL"
          ]
          Resource = ["${aws_s3_bucket.this.arn}/${local.inbound_sftp_s3_home_dir}*"]
        }
      ]
    }
  )
}

resource "aws_iam_role" "partner_bucket_role" {
  for_each = local.eft_partners_config

  name = "${local.full_name}-${each.key}-bucket-role"
  description = join("", [
    "Role granting cross-account permissions to partner-specific folder for ${each.key} within ",
    "the ${aws_s3_bucket.this.id} EFT bucket when role is assumed"
  ])

  assume_role_policy = jsonencode(
    {
      Statement = [
        for assumer_arn in each.value.bucket_iam_assumer_arns : {
          Effect = "Allow"
          Action = "sts:AssumeRole"
          Principal = {
            AWS = assumer_arn
          }
        }
      ]
      Version = "2012-10-17"
    }
  )
  managed_policy_arns = [aws_iam_policy.partner_bucket_access[each.key].arn]

  force_detach_policies = true
}

resource "aws_iam_policy" "partner_bucket_access" {
  for_each = local.eft_partners_config

  name = "${local.full_name}-${each.key}-allow-eft-s3-path"
  description = join("", [
    "Allows ${each.key} to access their specific EFT data when this policy's corresponding IAM ",
    "role is assumed by ${each.key}"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Sid    = "AllowListingOfPartnerHomePath"
          Effect = "Allow"
          Action = [
            "s3:ListBucket",
            "s3:GetBucketLocation"
          ]
          Resource = [aws_s3_bucket.this.arn]
          Condition = {
            StringLike = {
              "s3:prefix" = ["${each.value.bucket_home_path}/*"]
            }
          }
        },
        {
          Sid    = "AllowPartnerAccessToHomePath"
          Effect = "Allow"
          Action = [
            "s3:AbortMultipartUpload",
            "s3:DeleteObject",
            "s3:DeleteObjectVersion",
            "s3:GetObject",
            "s3:GetObjectAcl",
            "s3:GetObjectVersion",
            "s3:GetObjectVersionAcl",
            "s3:PutObject",
            "s3:PutObjectAcl",
            "s3:PutObjectVersionAcl"
          ],
          Resource = [
            "${aws_s3_bucket.this.arn}/${each.value.bucket_home_path}/*"
          ]
        },
        {
          Sid    = "AllowEncryptionAndDecryptionOfS3Files"
          Effect = "Allow"
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey",
          ]
          Resource = [
            local.kms_key_id
          ]
        },
      ]
    }
  )
}

resource "aws_iam_policy" "sftp_outbound_transfer_logs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-logs"
  description = join("", [
    "Permissions for the ${local.outbound_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "logs:CreateLogGroup"
          Resource = "arn:aws:logs:${local.region}:${local.account_id}:*"
        },
        {
          Effect = "Allow"
          Action = ["logs:CreateLogStream", "logs:PutLogEvents"]
          Resource = [
            "arn:aws:logs:${local.region}:${local.account_id}:log-group:/aws/lambda/${local.outbound_lambda_full_name}:*"
          ]
        }
      ]
    }
  )
}

resource "aws_iam_role" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name        = local.outbound_lambda_full_name
  path        = "/"
  description = "Role for ${local.outbound_lambda_full_name} Lambda"

  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Action = "sts:AssumeRole",
          Effect = "Allow",
          Principal = {
            Service = "lambda.amazonaws.com"
          }
        }
      ]
    }
  )

  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "logs_to_lambda_role" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  role       = one(aws_iam_role.sftp_outbound_transfer[*].name)
  policy_arn = one(aws_iam_policy.sftp_outbound_transfer_logs[*].arn)
}
