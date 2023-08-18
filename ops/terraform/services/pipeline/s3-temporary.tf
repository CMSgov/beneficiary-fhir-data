# These are temporary AWS S3 Resources for CCW's Usage in Verification
# planned changes in Q4 2022. This only applies to the prod environment.
# TODO: The following resources should be removed ca Q1 2023
resource "aws_s3_bucket" "ccw-verification" {
  count         = local.is_prod ? 1 : 0
  bucket        = "bfd-prod-ccw-verification"
  force_destroy = local.is_ephemeral_env
  # similar tags to the sole production etl user
  tags = {
    Layer   = local.layer,
    role    = local.legacy_service
    Note    = "Temporary resource to be removed ca Q1 2023"
    Purpose = "ETL PUT"
    UsedBy  = "CCW"
  }
}

resource "aws_s3_bucket_public_access_block" "ccw-verification" {
  count = local.is_prod ? 1 : 0

  bucket                  = aws_s3_bucket.ccw-verification[0].id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "ccw-verification" {
  count = local.is_prod ? 1 : 0

  bucket = aws_s3_bucket.ccw-verification[0].id
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_logging" "ccw-verification" {
  count = local.is_prod ? 1 : 0

  bucket        = aws_s3_bucket.ccw-verification[0].id
  target_bucket = local.logging_bucket
  target_prefix = "ccw_${local.legacy_service}_s3_access_logs/"
}
