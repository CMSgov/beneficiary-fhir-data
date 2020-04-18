data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "main" {
  bucket    = "bfd-insights-${var.sensitivity}-${data.aws_caller_identity.current.account_id}"
  acl       = "private"
  tags      = merge({sensitivity = var.sensitivity}, var.tags)

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm     = "AES256"
      }
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "main" {
  bucket = aws_s3_bucket.main.id

  block_public_acls   = true
  block_public_policy = true
  ignore_public_acls  = true
  restrict_public_buckets = true
}

## Folders in bucket

resource "aws_s3_bucket_object" "top" {
  for_each      = toset(["adhoc", "dasg", "projects", "users"])
  bucket        = aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "${each.value}/"
}

    