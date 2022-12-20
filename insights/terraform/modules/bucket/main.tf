data "aws_caller_identity" "current" {}

locals {
  full_name = "bfd-insights-${var.name}-${data.aws_caller_identity.current.account_id}"
  key_name  = "bfd-insights-${var.name}-cmk"
  account_id = data.aws_caller_identity.current.account_id
}

resource "aws_s3_bucket" "main" {
  bucket    = local.full_name
  acl       = "private"
  tags      = merge({sensitivity = var.sensitivity}, var.tags)

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

resource "aws_s3_bucket_public_access_block" "main" {
  bucket = aws_s3_bucket.main.id

  block_public_acls   = true
  block_public_policy = true
  ignore_public_acls  = true
  restrict_public_buckets = true
}

## Folders in bucket

resource "aws_s3_bucket_object" "top" {
  for_each      = toset(var.folders)
  bucket        = aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "${each.value}/"
}

# Every bucket has its own CMK which allows cross-acount 
resource "aws_kms_key" "main" {
  description   = "CMK for the ${local.full_name} bucket"
  tags          = var.tags
  key_usage     = "ENCRYPT_DECRYPT"
  is_enabled    = true
  policy        = length(var.cross_accounts) > 0 ? data.aws_iam_policy_document.cmk_policy.json : null
  enable_key_rotation = true
}

resource "aws_kms_alias" "main" {
  name          = "alias/${local.key_name}"
  target_key_id = aws_kms_key.main.key_id
}
