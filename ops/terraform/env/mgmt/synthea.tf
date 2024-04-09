## Set up the bucket and its configuration
resource "aws_s3_bucket" "synthea" {
  bucket = "bfd-mgmt-synthea"
}

resource "aws_s3_bucket_public_access_block" "synthea" {
  bucket = aws_s3_bucket.synthea.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "synthea" {
  bucket = aws_s3_bucket.synthea.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
