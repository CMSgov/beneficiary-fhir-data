##
# Moved from global/s3/temporary.tf
##

resource "aws_s3_bucket" "aws-glue-scripts" {
  bucket = "aws-glue-scripts-${local.account_id}-us-east-1"
}

resource "aws_s3_bucket_public_access_block" "aws-glue-scripts" {
  bucket = aws_s3_bucket.aws-glue-scripts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "aws-glue-scripts" {
  bucket = aws_s3_bucket.aws-glue-scripts.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "aws-athena-query-results" {
  bucket = "aws-athena-query-results-us-east-1-${local.account_id}"
}

resource "aws_s3_bucket_public_access_block" "aws-athena-query-results" {
  bucket = aws_s3_bucket.aws-athena-query-results.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}


resource "aws_s3_bucket_server_side_encryption_configuration" "aws-athena-query-results" {
  bucket = aws_s3_bucket.aws-athena-query-results.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "aws-glue-assets" {
  bucket = "aws-glue-assets-${local.account_id}-us-east-1"
}

resource "aws_s3_bucket_public_access_block" "aws-glue-assets" {
  bucket = aws_s3_bucket.aws-glue-assets.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "aws-glue-assets" {
  bucket = aws_s3_bucket.aws-glue-assets.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
