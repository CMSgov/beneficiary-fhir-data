resource "aws_s3_bucket" "this" {
  bucket        = local.is_ephemeral_env ? null : local.pipeline_bucket
  bucket_prefix = local.is_ephemeral_env ? "bfd-${local.env}-${local.legacy_service}" : null

  tags = {
    Layer = local.layer,
    role  = local.legacy_service
  }
}

# block public access to the bucket
resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.id
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_logging" "this" {
  count = local.is_ephemeral_env ? 0 : 1

  bucket        = aws_s3_bucket.this.id
  target_bucket = local.logging_bucket
  # TODO: correct the target prefix by adding a trailing '/'
  target_prefix = "${local.legacy_service}_s3_access_logs"
}

resource "aws_s3_bucket_acl" "this" {
  bucket = aws_s3_bucket.this.id
  acl    = "private"
}
