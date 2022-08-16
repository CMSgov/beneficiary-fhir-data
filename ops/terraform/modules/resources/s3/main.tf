## S3 Bucket - See README.md for more info.
#
locals {
  tags    = merge({ Layer = "data", role = var.role }, var.env_config.tags)
  is_prod = substr(var.env_config.env, 0, 4) == "prod" # matches prod and prod-sbx
  
  bucket_name    = "bfd-${var.env_config.env}-${var.role}-${data.aws_caller_identity.current.account_id}"
  logging_bucket = "bfd-${var.env_config.env}-logs-${data.aws_caller_identity.current.account_id}"
  logging_prefix = "bfd-${var.env_config.env}-${var.role}-${data.aws_caller_identity.current.account_id}/"
}

data "aws_caller_identity" "current" {}

## S3 Bucket
resource "aws_s3_bucket" "main" {
  bucket = var.id != null ? var.id : local.bucket_name
  tags   = local.tags
}

# bucket versioning
resource "aws_s3_bucket_versioning" "main" {
  count  = var.versioning_enabled == true ? 1 : 0
  bucket = aws_s3_bucket.main.bucket
  
  versioning_configuration {
    status = var.versioning == true ? "Enabled" : "Disabled"
  }
}

# lifecycle config
resource "aws_s3_bucket_lifecycle_configuration" "main" {
  count = var.lifecycle_enabled != null ? 1 : 0
  bucket = aws_s3_bucket.main.bucket

  # transition to infrequent access
  rule {
    id      = "transition-to-ia"
    enabled = var.transition_to_ia_days != null ? true : false

    transition {
      days          = var.transition_to_ia_days
      storage_class = "STANDARD_IA"
    }
  }

  # TODO: transition to glacier

  # transition noncurrent versions to IA
  rule {
    depends_on = [aws_s3_bucket_versioning.main]
    id      = "transition-noncurrent-versions-to-ia"
    enabled = var.lifecycle.ia_transition_noncurrent_versions_days != null ? true : false

    noncurrent_version_transition {
      days          = var.lifecycle.ia_transition_noncurrent_versions_days
      storage_class = "STANDARD_IA"
    }
  }

  ## TODO: transition noncurrent to glacier

  # expire noncurrent versions
  rule {
    depends_on = [aws_s3_bucket_versioning.main]
    id      = "expire-noncurrent-versions"
    enabled = var.lifecycle.noncurrent_version_expiration_days != null ? true : false

    noncurrent_version_expiration {
      days = var.lifecycle.noncurrent_version_expiration_days
    }
  }

  # remove aborted multipart uploads
  rule {
    id      = "remove-aborted-multipart-uploads"
    enabled = true

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }

}

# block public access
resource "aws_s3_bucket_public_access_block" "main" {
  bucket = aws_s3_bucket.main.bucket

  ignore_public_acls      = var.ignore_public_acls
  restrict_public_buckets = var.restrict_public_buckets
  block_public_acls       = var.block_public_acls
  block_public_policy     = var.block_public_policy
}

# acl
resource "aws_s3_bucket_acl" "main" {
  bucket = aws_s3_bucket.main.bucket
  acl = var.acl
}

# enable bucket logging
resource "aws_s3_bucket_logging" "main" {
  count  = var.logging_enabled == true ? 1 : 0
  bucket = aws_s3_bucket.main.bucket

  target_bucket = var.logging_bucket == null ? local.logging_bucket : var.logging_bucket
  target_prefix = var.logging_prefix == null ? local.logging_prefix : var.logging_prefix
}

# apply server side encryption by default
resource "aws_s3_bucket_server_side_encryption_configuration" "main" {
  bucket = aws_s3_bucket.main.bucket

  rule {
    apply_server_side_encryption_by_default {
      bucket_key_enabled = false
      sse_algorithm      = var.kms_key_id == "alias/aws/s3" ? "AES256" : "aws:kms"
      kms_master_key_id  = var.kms_key_id
    }

    # TODO: https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucket-key.html
    # bucket_key_enabled = true
  }
}
