resource "aws_s3_bucket" "admin" {
  bucket = "bfd-${local.env}-admin-${local.account_id}"

  tags = {
    role  = "admin"
    Layer = "data"
  }
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
  bucket = aws_s3_bucket.admin.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_logging" "admin" {
  bucket        = aws_s3_bucket.admin.id
  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "admin_s3_access_logs/"
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
  tags = {
    Layer = "data"
    role  = "logs"
  }
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
  bucket = aws_s3_bucket.logging.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
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

resource "aws_s3_bucket" "bfd_public_test_data" {
  bucket = "bfd-public-test-data"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "bfd_public_test_data" {
  bucket = aws_s3_bucket.bfd_public_test_data.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "bfd_public_test_data" {
  bucket = aws_s3_bucket.bfd_public_test_data.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "bfd_public_test_data" {
  bucket = aws_s3_bucket.bfd_public_test_data.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "bfd_public_test_data" {
  bucket = aws_s3_bucket.bfd_public_test_data.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "bfd_public_test_data" {
  depends_on = [aws_s3_bucket_versioning.bfd_public_test_data]
  bucket = aws_s3_bucket.bfd_public_test_data.id

  rule {
    id = "rule-1"

    filter {}

    transition {
      days            = 7
      storage_class   = "GLACIER_IR"
    }

    status = "Enabled"
  }
}