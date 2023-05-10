resource "aws_iam_role" "logs" {
  name = "${local.full_name}-logs"

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
  name = "${local.full_name}-eft-user"

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
  name = "${local.full_name}-eft-user"

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
          Resource = [aws_s3_bucket.this.arn],
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
          Resource = ["${aws_s3_bucket.this.arn}/${local.eft_user_username}*"]
        }
      ]
    }
  )
}
