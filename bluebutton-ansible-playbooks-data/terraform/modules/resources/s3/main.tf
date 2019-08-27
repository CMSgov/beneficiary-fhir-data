# Setup an S3 bucket.
#

locals {
  tags                    = merge({Layer="data", role=var.role}, var.env_config.tags)
  is_prod                 = substr(var.env_config.env, 0, 4) == "prod" 
}

data "aws_caller_identity" "current" {}

# Build a S3 bucket
#   - Encryption using a Customer Managed Key
#   - No versioning
#   - deletition protection in prod environments
#   - postfix with the account id to prevent global name conflicts
#
resource "aws_s3_bucket" "main" {
  bucket                  = "bfd-${var.env_config.env}-${var.role}-${data.aws_caller_identity.current.account_id}"
  acl                     = var.acl
  tags                    = local.tags

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = var.kms_key_id
        sse_algorithm     = "aws:kms"
      }
    }
  }

  dynamic "logging" {
    for_each        = var.log_bucket == "" ? [] : [var.log_bucket]
    content {
      target_bucket = logging.value
      target_prefix   = "logs/${var.role}"
    }
  }

  # TODO add retention policy
}
