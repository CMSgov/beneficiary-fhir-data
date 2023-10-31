
data "aws_canonical_user_id" "current" {}

# bfd-public-test-data S3 bucket imports, not currently managed with terraform
import {
  to = aws_s3_bucket.bfd-public-test-data
  id = "bfd-public-test-data"
}

import {
  to = aws_s3_bucket_server_side_encryption_configuration.bfd-public-test-data
  id = "bfd-public-test-data"
}

import {
  to = aws_s3_bucket_versioning.bfd-public-test-data
  id = "bfd-public-test-data"
}

resource "aws_s3_bucket" "bfd-public-test-data" {
  bucket = "bfd-public-test-data"

  tags = {
    "cms-cloud-exempt:public-s3-bucket" = "https://confluence.cms.gov/x/VFNBGg"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "bfd-public-test-data" {
  bucket = aws_s3_bucket.bfd-public-test-data.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "bfd-public-test-data" {
  bucket = aws_s3_bucket.bfd-public-test-data.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "bfd-public-test-data" {
  depends_on = [aws_s3_bucket_versioning.bfd-public-test-data]
  bucket = aws_s3_bucket.bfd-public-test-data.id

  rule {
    id = "rule-1"

    filter {}

    noncurrent_version_transition {
      noncurrent_days = 60
      storage_class   = "GLACIER"
    }

    status = "Enabled"
  }
}

# bfd-test-data S3 bucket configuration
resource "aws_s3_bucket" "bfd-test-data" {
  bucket = "bfd-test-data"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "bfd-test-data" {
  bucket = aws_s3_bucket.bfd-test-data.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "bfd-test-data" {
  bucket = aws_s3_bucket.bfd-test-data.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "bfd-test-data" {
  bucket = aws_s3_bucket.bfd-test-data.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "bfd-test-data" {
  depends_on = [aws_s3_bucket_versioning.bfd-test-data]
  bucket = aws_s3_bucket.bfd-test-data.id

  rule {
    id = "rule-1"

    filter {}

    noncurrent_version_transition {
      noncurrent_days = 60
      storage_class   = "GLACIER"
    }

    status = "Enabled"
  }
}
