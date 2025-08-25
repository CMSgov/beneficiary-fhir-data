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

  tags = var.tags
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

  # We yield to the bucket key for unencrypted put requests, so the following statements ensure that
  # an operator cannot specify a different key or type of encryption other than what is expected
  statement {
    sid       = "DenyIncorrectKmsKeyIfSpecified"
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

    condition {
      test     = "Null"
      variable = "s3:x-amz-server-side-encryption-aws-kms-key-id"
      values   = [false]
    }
  }

  statement {
    sid       = "DenyNonKMSPutsIfSSESpecified"
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

    condition {
      test     = "Null"
      variable = "s3:x-amz-server-side-encryption"
      values   = [false]
    }
  }
}

data "aws_iam_policy_document" "combined" {
  source_policy_documents = concat([data.aws_iam_policy_document.this.json], coalesce(var.additional_bucket_policy_docs, []))
}

resource "aws_s3_bucket_policy" "this" {
  bucket = aws_s3_bucket.this.bucket
  policy = data.aws_iam_policy_document.combined.json
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

resource "aws_ssm_parameter" "bucket_name" {
  count = var.ssm_param_name != null && trimspace(var.ssm_param_name) != "" ? 1 : 0

  name           = var.ssm_param_name
  tier           = "Intelligent-Tiering"
  type           = "String"
  insecure_value = aws_s3_bucket.this.bucket

  tags = {
    for_bucket    = aws_s3_bucket.this.bucket
    DO_NOT_MODIFY = true
  }
}
