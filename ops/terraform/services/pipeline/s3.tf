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
  # After April 2023, new S3 Buckets have public access disabled along with ACLs disabled. This
  # resource will fail to apply for ephemeral environments (new buckets)
  # FIXME: Replace/resolve this before accepting BFD-2554
  count = local.is_ephemeral_env ? 0 : 1

  bucket        = aws_s3_bucket.this.id
  target_bucket = local.logging_bucket

  # TODO: correct the target prefix by adding a trailing '/'
  target_prefix = "${local.legacy_service}_s3_access_logs"
}

resource "aws_s3_bucket_acl" "this" {
  # After April 2023, new S3 Buckets have public access disabled along with ACLs disabled. This
  # resource will fail to apply for ephemeral environments
  # FIXME: Replace/resolve this after accepting BFD-2554
  count = local.is_ephemeral_env ? 0 : 1

  bucket = aws_s3_bucket.this.id
  acl    = "private"
}

resource "aws_s3_bucket_notification" "etl_bucket_notifications" {
  bucket = aws_s3_bucket.this.id

  # Lambda function notifications for the BFD Pipeline SLIs Lambda
  dynamic "lambda_function" {
    for_each = {
      for prefix in ["Incoming", "Done"] : "${prefix}/" => lower(prefix)
      if local.create_slis # Only create notifications for SLIs lambda if it's being created
    }

    content {
      events              = ["s3:ObjectCreated:*"]
      filter_prefix       = lambda_function.key
      id                  = "${module.bfd_pipeline_slis[0].lambda_name}-${lambda_function.value}"
      lambda_function_arn = module.bfd_pipeline_slis[0].lambda_arn
    }
  }

  # Lambda function notifications for the BFD Pipeline Scheduler Lambda
  dynamic "lambda_function" {
    for_each = {
      for prefix in ["Incoming", "Done", "Synthetic/Incoming", "Synthetic/Done"] : "${prefix}/" => lower(replace(prefix, "/", "-"))
      if local.pipeline_variant_configs.ccw.enabled # Only create if CCW pipeline is enabled
    }

    content {
      events              = ["s3:ObjectCreated:*", "s3:ObjectRemoved:*"]
      filter_prefix       = lambda_function.key
      id                  = "${module.bfd_pipeline_scheduler[0].lambda_name}-${lambda_function.value}"
      lambda_function_arn = module.bfd_pipeline_scheduler[0].lambda_arn
    }
  }
}
