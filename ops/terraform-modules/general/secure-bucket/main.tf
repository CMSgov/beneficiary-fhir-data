resource "aws_s3_bucket" "this" {
  lifecycle {
    precondition {
      condition     = !(var.bucket_prefix != null && var.bucket_name != null)
      error_message = "Both `bucket_prefix` and `bucket_name` cannot be specified at the same time."
    }
  }

  bucket_prefix = var.bucket_prefix
  bucket        = var.bucket_name
  force_destroy = var.force_destroy
}

data "aws_iam_policy_document" "this" {
  statement {
    sid       = "DenyInsecureTransportOperations"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = [aws_s3_bucket.this.arn, "${aws_s3_bucket.this.arn}/*"]

    principals {
      identifiers = ["*"]
      type        = "*"
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }

  statement {
    sid       = "DenyIncorrectKmsKey"
    effect    = "Deny"
    actions   = ["s3:PutObject"]
    resources = [aws_s3_bucket.this.arn, "${aws_s3_bucket.this.arn}/*"]

    principals {
      identifiers = ["*"]
      type        = "*"
    }

    condition {
      test     = "StringNotEquals"
      variable = "s3:x-amz-server-side-encryption-aws-kms-key-id"
      values   = [var.bucket_kms_key_arn]
    }
  }

  statement {
    sid       = "DenyUnencryptedPuts"
    effect    = "Deny"
    actions   = ["s3:PutObject"]
    resources = [aws_s3_bucket.this.arn, "${aws_s3_bucket.this.arn}/*"]

    principals {
      identifiers = ["*"]
      type        = "*"
    }

    condition {
      test     = "Null"
      variable = "s3:x-amz-server-side-encryption"
      values   = [true]
    }
  }

  statement {
    sid       = "DenyNonKMSPuts"
    effect    = "Deny"
    actions   = ["s3:PutObject"]
    resources = [aws_s3_bucket.this.arn, "${aws_s3_bucket.this.arn}/*"]

    principals {
      identifiers = ["*"]
      type        = "*"
    }

    condition {
      test     = "StringNotEquals"
      variable = "s3:x-amz-server-side-encryption"
      values   = ["aws:kms"]
    }
  }
}

resource "aws_s3_bucket_policy" "this" {
  bucket = aws_s3_bucket.this.bucket
  policy = data.aws_iam_policy_document.this.json
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.bucket

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.bucket

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = var.bucket_kms_key_arn
      sse_algorithm     = "aws:kms"
    }

    bucket_key_enabled = true
  }
}
