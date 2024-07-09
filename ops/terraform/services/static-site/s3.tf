resource "aws_s3_bucket" "cloudfront_bucket" {
  bucket        = local.is_ephemeral_env ? null : local.static_cf_bucket_name
  #bucket_prefix = local.is_ephemeral_env ? null : "static"

  tags = {
    Layer = "static-${local.layer}",
    role  = local.legacy_service
  }
}

resource "aws_s3_bucket_ownership_controls" "cloudfront_bucket" {
  bucket = aws_s3_bucket.cloudfront_bucket.bucket
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_website_configuration" "static" {
  bucket = aws_s3_bucket.cloudfront_bucket.bucket

  index_document {
    suffix = "index.html"
  }

}

# block public access to the bucket
resource "aws_s3_bucket_public_access_block" "cloudfront_bucket" {
  bucket                  = aws_s3_bucket.cloudfront_bucket.bucket
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudfront_bucket" {
  bucket = aws_s3_bucket.cloudfront_bucket.bucket
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id  
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_logging" "cloudfront_bucket" {
  bucket = aws_s3_bucket.cloudfront_bucket.bucket

  target_bucket = aws_s3_bucket.cloudfront_logging.bucket  # local.logging_bucket
  target_prefix = "${terraform.workspace}-${local.legacy_service}_s3_access_logs/"
}

resource "aws_s3_bucket_policy" "cloudfront_bucket" {
  bucket = aws_s3_bucket.cloudfront_bucket.bucket

  policy = data.aws_iam_policy_document.cf_bucket_policy.json
}

resource "aws_s3_object" "index" {
  bucket  = aws_s3_bucket.cloudfront_bucket.bucket
  key     = "/index.html"
  
  content_type = "text/html"
  content      = <<EOF
<!DOCTYPE html>
<html>
<head>${terraform.workspace} Index page</head>
<body>
<p>Placeholder page for ${terraform.workspace} Static Site</p>
</body>
</html>
EOF

  lifecycle {
    ignore_changes = [ content, tags ]
  }
}

resource "aws_s3_object" "error" {
  bucket  = aws_s3_bucket.cloudfront_bucket.bucket
  key     = "/error.html"
  
  content_type = "text/html"
  content      = <<EOF
<!DOCTYPE html>
<html>
<head>${terraform.workspace} Error page</head>
<body>
<p>Placeholder Error page for ${terraform.workspace} Static Site</p>
</body>
</html>
EOF

  lifecycle {
    ignore_changes = [ content, tags ]
  }
}

resource "aws_s3_bucket" "cloudfront_logging" {
  bucket = local.static_cflog_bkt_name # local.logging_bucket
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
  bucket     = aws_s3_bucket.cloudfront_logging.bucket
  depends_on = [ aws_s3_bucket_ownership_controls.cloudfront_logging ]
  acl        = "log-delivery-write"
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

  policy = data.aws_iam_policy_document.cf_logging_policy.json
}
