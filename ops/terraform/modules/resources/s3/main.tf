# Setup an S3 bucket.
#

locals {
  tags    = merge({ Layer = "data", role = var.role }, var.env_config.tags)
  is_prod = substr(var.env_config.env, 0, 4) == "prod"
}

data "aws_caller_identity" "current" {}

# Build a S3 bucket
#   - Encryption using a Customer Managed Key
#   - No versioning
#   - deletition protection in prod environments
#   - postfix with the account id to prevent global name conflicts
#
resource "aws_s3_bucket" "main" {
  bucket = "bfd-${var.env_config.env}-${var.role}-${data.aws_caller_identity.current.account_id}"
  acl    = var.acl
  tags   = local.tags

  # Always apply encryption, Customer CMK or AWS AES256
  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm     = var.kms_key_id != null ? "aws:kms" : "AES256"
        kms_master_key_id = var.kms_key_id
      }
    }
  }

  # Add logging if specified
  dynamic "logging" {
    for_each = var.log_bucket == "" ? [] : [var.log_bucket]
    content {
      target_bucket = logging.value
      target_prefix = "${var.role}_s3_access_logs"
    }
  }

  # TODO add retention policy
}

# For safety, block public access to the bucket
#
resource "aws_s3_bucket_public_access_block" "main" {
  bucket = aws_s3_bucket.main.id

  block_public_acls   = true
  block_public_policy = true
}
