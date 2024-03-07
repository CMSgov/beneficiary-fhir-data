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
        for index, assumer_arn in each.value.bucket_iam_assumer_arns : {
          Sid    = "AllowAssumeRole${index}"
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

resource "aws_iam_policy" "sftp_outbound_transfer_ssm" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name        = "${local.outbound_lambda_full_name}-ssm"
  description = "Permissions to get parameters from the appropriate hierarchies"
  policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Effect = "Allow",
          Action = [
            "ssm:GetParametersByPath",
            "ssm:GetParameters",
            "ssm:GetParameter"
          ],
          Resource = [
            for hierarchy in local.ssm_hierarchies :
            "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(hierarchy, "/")}/*"
          ]
        }
      ]
    }
  )
}

resource "aws_iam_policy" "sftp_outbound_transfer_kms" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-kms"
  description = join("", [
    "Permissions to decrypt config KMS keys and encrypt and decrypt master KMS keys for ",
    "${local.env}"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Sid    = "AllowEncryptionAndDecryptionOfConfigKeys"
          Effect = "Allow",
          Action = [
            "kms:Decrypt"
          ],
          Resource = local.kms_config_key_arns
        },
        {
          Sid    = "AllowEncryptionAndDecryptionOfMasterKeys"
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

resource "aws_iam_policy" "sftp_outbound_transfer_s3" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-s3"
  description = join("", [
    "Allows the ${local.outbound_lambda_full_name} to manipulate objects within the ",
    "${local.full_name} S3 bucket",
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Sid    = "AllowListingOfBucket"
          Effect = "Allow"
          Action = [
            "s3:ListBucket",
            "s3:GetBucketLocation"
          ]
          Resource = [aws_s3_bucket.this.arn]
          Condition = {
            StringLike = {
              "s3:prefix" = [
                for partner in local.eft_partners_with_outbound_enabled :
                "${aws_s3_bucket.this.arn}/${local.eft_partners_config[partner].outbound.pending_path}/*"
              ]
            }
          }
        },
        {
          Sid    = "AllowAccessToOutboundPaths"
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
            for partner in local.eft_partners_with_outbound_enabled :
            "${aws_s3_bucket.this.arn}/${local.eft_partners_config[partner].outbound.pending_path}/*"
          ]
        },
      ]
    }
  )
}

resource "aws_iam_policy" "sftp_outbound_transfer_sqs_dlq" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-sqs-dlq"
  description = join("", [
    "Allows the ${local.outbound_lambda_full_name} to push events into its DLQ upon any failures"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Sid    = "AllowSendingMessages"
          Effect = "Allow"
          Action = [
            "sqs:GetQueueUrl",
            "sqs:SendMessage"
          ]
          Resource = [one(aws_sqs_queue.sftp_outbound_transfer_dlq[*].arn)]
        },
      ]
    }
  )
}

resource "aws_iam_policy" "sftp_outbound_transfer_sns" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_lambda_full_name}-sns"
  description = join("", [
    "Allows the ${local.outbound_lambda_full_name} to publish status notifications to the ",
    "${local.outbound_notifs_topic_prefix} SNS Topic and partner-specific Topics"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Sid    = "AllowSendingMessages"
          Effect = "Allow"
          Action = [
            "SNS:Publish"
          ]
          Resource = flatten([
            aws_sns_topic.outbound_notifs[*].arn,
            [
              for partner in local.eft_partners_with_outbound_notifs :
              aws_sns_topic.outbound_partner_notifs[partner].arn
            ]
          ])
        },
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

resource "aws_iam_role_policy_attachment" "sftp_outbound_transfer" {
  for_each = {
    for key, arn in {
      logs    = one(aws_iam_policy.sftp_outbound_transfer_logs[*].arn),
      ssm     = one(aws_iam_policy.sftp_outbound_transfer_ssm[*].arn),
      kms     = one(aws_iam_policy.sftp_outbound_transfer_kms[*].arn),
      s3      = one(aws_iam_policy.sftp_outbound_transfer_s3[*].arn),
      sqs_dlq = one(aws_iam_policy.sftp_outbound_transfer_sqs_dlq[*].arn)
      sns     = one(aws_iam_policy.sftp_outbound_transfer_sns[*].arn)
      vpc     = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
    } : key => arn
    if length(local.eft_partners_with_outbound_enabled) > 0
  }

  role       = one(aws_iam_role.sftp_outbound_transfer[*].name)
  policy_arn = each.value
}

resource "aws_iam_policy" "outbound_notifs_logs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name = "${local.outbound_notifs_topic_prefix}-logs"
  description = join("", [
    "Permissions for the ${local.outbound_notifs_topic_prefix} SNS topic to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "logs:CreateLogGroup",
            "logs:CreateLogStream",
            "logs:PutLogEvents",
            "logs:PutMetricFilter",
            "logs:PutRetentionPolicy"
          ]
          Resource = [
            # There is no documentation about SNS delivery status logging that explicitly defines
            # the log group naming format. So, constraining this policy is not entirely possible
            "arn:aws:logs:${local.region}:${local.account_id}:*"
          ]
        }
      ]
    }
  )
}

resource "aws_iam_policy" "outbound_partner_notifs_logs" {
  for_each = toset(local.eft_partners_with_outbound_notifs)

  name = "${local.outbound_notifs_topic_prefix}-${each.key}-logs"
  description = join("", [
    "Permissions for the ${local.outbound_notifs_topic_prefix}-${each.key} SNS topic to write to ",
    "its corresponding CloudWatch Log Group and Log Stream",
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "logs:CreateLogGroup",
            "logs:CreateLogStream",
            "logs:PutLogEvents",
            "logs:PutMetricFilter",
            "logs:PutRetentionPolicy"
          ]
          Resource = [
            # There is no documentation about SNS delivery status logging that explicitly defines
            # the log group naming format. So, constraining this policy is not entirely possible
            "arn:aws:logs:${local.region}:${local.account_id}:*"
          ]
        }
      ]
    }
  )
}

resource "aws_iam_role" "outbound_notifs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  name        = local.outbound_notifs_topic_prefix
  path        = "/"
  description = "Role for ${local.outbound_notifs_topic_prefix} SNS Topic"

  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Action = "sts:AssumeRole",
          Effect = "Allow",
          Principal = {
            Service = "sns.amazonaws.com"
          }
        }
      ]
    }
  )

  force_detach_policies = true
}

resource "aws_iam_role" "outbound_partner_notifs" {
  for_each = toset(local.eft_partners_with_outbound_notifs)

  name        = "${local.outbound_notifs_topic_prefix}-${each.key}"
  path        = "/"
  description = "Role for ${local.outbound_notifs_topic_prefix}-${each.key} SNS Topic"

  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Action = "sts:AssumeRole",
          Effect = "Allow",
          Principal = {
            Service = "sns.amazonaws.com"
          }
        }
      ]
    }
  )

  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "outbound_notifs" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  role       = one(aws_iam_role.outbound_notifs[*].name)
  policy_arn = one(aws_iam_policy.outbound_notifs_logs[*].arn)
}

resource "aws_iam_role_policy_attachment" "outbound_partner_notifs" {
  for_each = toset(local.eft_partners_with_outbound_notifs)

  role       = aws_iam_role.outbound_partner_notifs[each.key].name
  policy_arn = aws_iam_policy.outbound_partner_notifs_logs[each.key].arn
}
