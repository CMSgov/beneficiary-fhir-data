data "aws_caller_identity" "current" {}

locals {
  full_name  = "bfd-insights-${var.name}-${data.aws_caller_identity.current.account_id}"
  key_name   = "bfd-insights-${var.name}-cmk"
  account_id = data.aws_caller_identity.current.account_id
}

# Main S3 bucket
resource "aws_s3_bucket" "main" {
  bucket = local.full_name
  tags   = merge({ sensitivity = var.sensitivity }, var.tags) # TODO: is sensitivity needed?

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm     = "aws:kms"
        kms_master_key_id = aws_kms_key.main.arn
      }
      bucket_key_enabled = var.bucket_key_enabled
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}

# Public access block configuration for the main S3 bucket
resource "aws_s3_bucket_public_access_block" "main" {
  bucket = aws_s3_bucket.main.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

## Create any top level folders
resource "aws_s3_bucket_object" "top" {
  for_each     = toset(var.folders)
  bucket       = aws_s3_bucket.main.id
  content_type = "application/x-directory"
  key          = "${each.value}/"
}

# KMS key configuration (CMK)
resource "aws_kms_key" "main" {
  description         = "CMK for the ${local.full_name} bucket"
  key_usage           = "ENCRYPT_DECRYPT"
  is_enabled          = true
  policy              = length(var.cross_accounts) > 0 ? data.aws_iam_policy_document.cmk_policy.json : null
  enable_key_rotation = true
  tags                = var.tags

  # When/if we enable bucket keys, it will only apply to new objects, so we need to ensure we don't delete the cmk until
  # all objects encrypted with the cmk are either deleted or re-encrypted with the bucket key.
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_kms_alias" "main" {
  name          = "alias/${local.key_name}"
  target_key_id = aws_kms_key.main.key_id
}
