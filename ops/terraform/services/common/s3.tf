resource "aws_s3_bucket" "this" {
  bucket        = local.is_ephemeral_env ? null : local.admin_bucket
  bucket_prefix = local.is_ephemeral_env ? "bfd-${local.env}-${local.legacy_service}" : null

  tags = {
    Layer = local.layer,
    role  = local.legacy_service
  }
}

# block public access to the bucket
resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.id
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_logging" "this" {
  # TODO: Make this work better for ephemeral environments, etc
  count = local.is_ephemeral_env ? 0 : 1

  bucket = aws_s3_bucket.this.id

  # TODO: consider adding this...
  # expected_bucket_owner = local.account_id

  target_bucket = local.logging_bucket
  target_prefix = "${local.legacy_service}_s3_access_logs/"
}

resource "aws_s3_bucket_acl" "this" {
  # After April 2023, new S3 Buckets have public access disabled along with ACLs disabled. This
  # resource will fail to apply for ephemeral environments (new buckets)
  # FIXME: Replace/resolve this before accepting BFD-2554
  count = local.is_ephemeral_env ? 0 : 1

  bucket = aws_s3_bucket.this.id
  acl    = "private"
}

resource "aws_s3_bucket" "logging" {
  bucket = local.logging_bucket
  tags = {
    Layer = local.layer
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
  # After April 2023, new S3 Buckets have public access disabled along with ACLs disabled. This
  # resource will fail to apply for ephemeral environments
  # TODO: Replace/resolve this in BFD-2554
  count = local.is_ephemeral_env ? 0 : 1

  acl    = "log-delivery-write"
  bucket = aws_s3_bucket.logging.id
}

resource "aws_s3_bucket_policy" "logging" {
  bucket = aws_s3_bucket.logging.id
  policy = <<POLICY
{
  "Id": "LBAccessLogs",
  "Statement": [
    {
      "Action": "s3:PutObject",
      "Effect": "Allow",
      "Principal": {
        "AWS": "${local.aws_classic_loadbalancer_account_roots[local.region]}"
      },
      "Resource": "arn:aws:s3:::bfd-${local.env}-logs-${local.account_id}/*"
    },
    {
      "Action": "s3:*",
      "Condition": {
        "Bool": {
          "aws:SecureTransport": "false"
        }
      },
      "Effect": "Deny",
      "Principal": "*",
      "Resource": [
        "arn:aws:s3:::bfd-${local.env}-logs-${local.account_id}",
        "arn:aws:s3:::bfd-${local.env}-logs-${local.account_id}/*"
      ],
      "Sid": "AllowSSLRequestsOnly"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}
