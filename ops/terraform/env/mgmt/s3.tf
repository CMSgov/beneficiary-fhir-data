resource "aws_s3_bucket" "admin" {
  bucket = "bfd-${local.env}-admin-${local.account_id}"

  tags = merge(local.shared_tags, {
    role  = "admin"
    Layer = "data"
  })
}

resource "aws_s3_bucket_server_side_encryption_configuration" "admin" {
  bucket = aws_s3_bucket.admin.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = local.kms_key_id
    }
  }
}

resource "aws_s3_bucket_public_access_block" "admin" {
  block_public_acls       = true
  block_public_policy     = true
  bucket                  = aws_s3_bucket.admin.id
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_logging" "admin" {
  bucket        = aws_s3_bucket.admin.id
  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "admin_s3_access_logs"
}

resource "aws_s3_bucket_acl" "admin" {
  acl    = "private"
  bucket = aws_s3_bucket.admin.id
}

resource "aws_s3_bucket_policy" "admin" {
  bucket = aws_s3_bucket.admin.id
  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowSSLRequestsOnly",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": [
                "${aws_s3_bucket.admin.arn}",
                "${aws_s3_bucket.admin.arn}/*"
            ],
            "Condition": {
                "Bool": {
                    "aws:SecureTransport": "false"
                }
            }
        }
    ]
}
POLICY
}

resource "aws_s3_bucket" "logging" {
  bucket = "bfd-${local.env}-logs-${local.account_id}"
  tags = merge(local.shared_tags, {
    Layer = "data"
    role  = "logs"
  })
}

resource "aws_s3_bucket_server_side_encryption_configuration" "logging" {
  bucket = aws_s3_bucket.logging.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "logging" {
  block_public_acls       = true
  block_public_policy     = true
  bucket                  = aws_s3_bucket.logging.id
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_acl" "logging" {
  acl    = "log-delivery-write"
  bucket = aws_s3_bucket.logging.id
}

resource "aws_s3_bucket_policy" "logging" {
  bucket = aws_s3_bucket.logging.id
  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowSSLRequestsOnly",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": [
                "${aws_s3_bucket.logging.arn}",
                "${aws_s3_bucket.logging.arn}/*"
            ],
            "Condition": {
                "Bool": {
                    "aws:SecureTransport": "false"
                }
            }
        }
    ]
}
POLICY
}
