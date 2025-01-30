# ETL Pipeline S3 Bucket
resource "aws_s3_bucket" "this" {
  bucket        = local.is_ephemeral_env ? null : local.pipeline_bucket
  bucket_prefix = local.is_ephemeral_env ? "bfd-${local.env}-${local.legacy_service}" : null
  force_destroy = local.is_ephemeral_env

  tags = {
    Layer = local.layer,
    role  = local.legacy_service
  }
}

# block public access to the bucket
resource "aws_s3_bucket_public_access_block" "this" {
  bucket                  = aws_s3_bucket.this.id
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
  target_prefix = "${local.legacy_service}_s3_access_logs/"
}

resource "aws_s3_bucket_notification" "etl_bucket_notifications" {
  count  = local.pipeline_variant_configs.ccw.enabled ? 1 : 0
  bucket = aws_s3_bucket.this.id

  dynamic "topic" {
    for_each = {
      "Incoming"           = ["s3:ObjectCreated:*", "s3:ObjectRemoved:*"]
      "Done"               = ["s3:ObjectCreated:*"]
      "Synthetic/Incoming" = ["s3:ObjectCreated:*", "s3:ObjectRemoved:*"]
      "Synthetic/Done"     = ["s3:ObjectCreated:*"]
    }

    content {
      events        = topic.value
      filter_prefix = "${topic.key}/"
      id            = "${aws_sns_topic.s3_events["ccw"].name}-${lower(replace(topic.key, "/", "-"))}"
      topic_arn     = aws_sns_topic.s3_events["ccw"].arn
    }
  }
}

# CCW Verification Bucket
resource "aws_s3_bucket" "ccw-verification" {
  count  = local.is_prod ? 1 : 0
  bucket = "bfd-prod-ccw-verification"
  tags = {
    Layer   = local.layer,
    role    = local.legacy_service
    Note    = "Pre-production sensitive data used for CCW and BFD process verification"
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
