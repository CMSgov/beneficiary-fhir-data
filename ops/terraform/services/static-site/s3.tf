resource "aws_s3_bucket" "static_site" {
  bucket = local.is_ephemeral_env ? null : local.static_cloudfront_name

  tags = {
    Layer = "static-${local.layer}",
    role  = local.legacy_service
  }
}

resource "aws_s3_bucket_ownership_controls" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_website_configuration" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket

  index_document {
    suffix = "index.html"
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
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_logging" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket

  target_bucket = aws_s3_bucket.vpce_alb_logging.bucket # local.logging_bucket
  target_prefix = "${local.full_name}_s3_access_logs/"
}

resource "aws_s3_bucket_policy" "static_site" {
  bucket = aws_s3_bucket.static_site.bucket

  policy = data.aws_iam_policy_document.s3_static_policy.json
}

resource "aws_s3_object" "index" {
  bucket = aws_s3_bucket.static_site.bucket
  key    = "/index.html"

  content_type = "text/html"
  content      = <<EOF
<!DOCTYPE html>
<html>
  <head>
    ${local.static_site_fqdn} Index page
  </head>
  <body>
    <p>Placeholder page for ${local.static_site_fqdn} Static Site</p>
  </body>
</html>

EOF

  lifecycle {
    ignore_changes = [content, tags]
  }
}

resource "aws_s3_object" "error" {
  bucket = aws_s3_bucket.static_site.bucket
  key    = "/error.html"

  content_type = "text/html"
  content      = <<EOF
<!DOCTYPE html>
<html>
  <head>
    ${local.static_site_fqdn} Error page
  </head>
  <body>
    <p>Placeholder Error page for ${local.static_site_fqdn} Static Site</p>
  </body>
</html>

EOF

  lifecycle {
    ignore_changes = [content, tags]
  }
}

resource "aws_s3_bucket" "vpce_alb_logging" {
  bucket = local.static_logging_name # local.logging_bucket
  tags = {
    Layer = "static-${local.layer}"
    role  = "logs"
  }
}

resource "aws_s3_bucket_public_access_block" "vpce_alb_logging" {
  bucket                  = aws_s3_bucket.vpce_alb_logging.bucket
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "vpce_alb_logging" {
  bucket = aws_s3_bucket.vpce_alb_logging.bucket
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "vpce_alb_logging" {
  bucket     = aws_s3_bucket.vpce_alb_logging.bucket
  depends_on = [aws_s3_bucket_ownership_controls.vpce_alb_logging]
  acl        = "log-delivery-write"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "vpce_alb_logging" {
  bucket = aws_s3_bucket.vpce_alb_logging.bucket
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_policy" "vpce_alb_logging" {
  bucket = aws_s3_bucket.vpce_alb_logging.bucket

  policy = data.aws_iam_policy_document.vpce_alb_log_policy.json
}
