## Setup an S3 bucket intended for PII
locals {
  tags = {
    Layer = "data"
    role  = var.pii_bucket_config.name
  }
  account_id = data.aws_caller_identity.current.account_id
}

data "aws_caller_identity" "current" {}

## S3 Bucket
#

resource "aws_kms_key" "pii_bucket_key" {
  description             = "bfd-${var.env}-${var.pii_bucket_config.name}-cmk"
  deletion_window_in_days = 10
  enable_key_rotation     = true

  policy = templatefile("${path.module}/templates/kms-policy.json", {
    env     = var.env
    name    = var.pii_bucket_config.name
    admins  = var.pii_bucket_config.admin_arns
    readers = var.pii_bucket_config.read_arns
    writers = [aws_iam_role.pii_bucket_writer_role.arn]
    root    = ["arn:aws:iam::${local.account_id}:root"]
  })
}

resource "aws_kms_alias" "pii_bucket_key_alias" {
  name          = "alias/bfd-${var.env}-${var.pii_bucket_config.name}-cmk"
  target_key_id = aws_kms_key.pii_bucket_key.id
}

resource "aws_s3_bucket" "pii_bucket" {
  bucket = "bfd-${var.env}-${var.pii_bucket_config.name}-${local.account_id}"
  acl    = "private"
  tags   = local.tags

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm     = "aws:kms"
        kms_master_key_id = aws_kms_key.pii_bucket_key.id
      }
    }
  }

  logging {
    target_bucket = var.pii_bucket_config.log_bucket
    target_prefix = "${var.pii_bucket_config.name}_s3_access_logs/"
  }

  versioning {
    enabled = true
  }

  lifecycle_rule {
    id      = "bfd-${var.env}-${var.pii_bucket_config.name}-versioning-rule"
    enabled = true

    noncurrent_version_transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    noncurrent_version_expiration {
      days = 60
    }
  }
}

resource "aws_s3_bucket_public_access_block" "pii_bucket_restrictions" {
  bucket = aws_s3_bucket.pii_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_policy" "pii_bucket_policy" {
  bucket = aws_s3_bucket.pii_bucket.id

  policy = templatefile("${path.module}/templates/bucket-policy.json", {
    env         = var.env
    bucket_id   = aws_s3_bucket.pii_bucket.id
    bucket_name = var.pii_bucket_config.name
    admins      = var.pii_bucket_config.admin_arns
    readers     = var.pii_bucket_config.read_arns
    writers     = [aws_iam_role.pii_bucket_writer_role.arn]
    root        = ["arn:aws:iam::${local.account_id}:root"]
  })
}

resource "aws_iam_role" "pii_bucket_writer_role" {
  name = "bfd-${var.env}-${var.pii_bucket_config.name}-writer-role"

  assume_role_policy = templatefile("${path.module}/templates/iam-writer-assume-policy.json", {
    env     = var.env
    name    = var.pii_bucket_config.name
    writers = var.pii_bucket_config.write_accts
  })
}

resource "aws_iam_policy" "pii_bucket_writer_policy" {
  name = "bfd-${var.env}-${var.pii_bucket_config.name}-writer-policy"

  policy = templatefile("${path.module}/templates/iam-writer-policy.json", {
    env       = var.env
    name      = var.pii_bucket_config.name
    bucket_id = aws_s3_bucket.pii_bucket.id
  })
}

resource "aws_iam_role_policy_attachment" "pii_bucket_writer_policy_attachment" {
  role       = aws_iam_role.pii_bucket_writer_role.name
  policy_arn = aws_iam_policy.pii_bucket_writer_policy.arn
}
