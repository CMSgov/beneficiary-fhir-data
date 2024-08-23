resource "aws_s3_bucket" "static_site" {
  bucket = local.is_ephemeral_env ? null : local.static_cloudfront_name

  tags = {
    Layer = "static-${local.layer}",
    role  = local.service
  }
}

resource "aws_s3_bucket_ownership_controls" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

# block public access to the bucket
resource "aws_s3_bucket_public_access_block" "static_site" {
  bucket                  = aws_s3_bucket.static_site.bucket
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_logging" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket

  target_bucket = aws_s3_bucket.cloudfront_logging.bucket
  target_prefix = "${local.env}-${local.service}_s3_access_logs/"
}

resource "aws_s3_bucket_policy" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket

  policy = data.aws_iam_policy_document.cloudfront_policy.json
}

resource "aws_s3_bucket" "cloudfront_logging" {
  bucket = local.static_logging_name
  tags = {
    Layer = "static-${local.layer}"
    role  = "logs"
  }
}

resource "aws_s3_bucket_public_access_block" "cloudfront_logging" {
  bucket                  = aws_s3_bucket.cloudfront_logging.bucket
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "cloudfront_logging" {
  bucket = aws_s3_bucket.cloudfront_logging.bucket
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "cloudfront_logging" {
  bucket = aws_s3_bucket.cloudfront_logging.bucket
  acl    = "log-delivery-write"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudfront_logging" {
  bucket = aws_s3_bucket.cloudfront_logging.bucket
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_policy" "cloudfront_logging" {
  bucket = aws_s3_bucket.cloudfront_logging.bucket

  policy = data.aws_iam_policy_document.cloudfront_log_policy.json
}
