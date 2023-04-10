locals {
  # one logging bucket per environment, we will use prefixes maintain ephemeral logs in-memoriam
  logging_bucket = "bfd-${local.data_env}-logs-${local.account_id}"
  logging_prefix = local.is_ephemeral_env ? replace("${local.stack}_${local.legacy_service}_s3_access_logs/", "-", "_") : "${local.legacy_service}_s3_access_logs/"

  # one admin bucket per project
  # TODO: what's the purpose of of the legacy service here?
  admin_bucket = "${local.stack}-${local.legacy_service}-${local.account_id}"

  # NOTE: AWS Account Roots for Access Log Delivery
  # https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/enable-access-logs.html
  aws_classic_loadbalancer_account_roots = {
    us-east-1 = "arn:aws:iam::127311923021:root"
    us-west-2 = "arn:aws:iam::797873946194:root"
  }
}

resource "aws_s3_bucket" "admin" {
  bucket = local.admin_bucket
  tags = {
    role = local.legacy_service
  }
}

# block public access to the bucket
resource "aws_s3_bucket_public_access_block" "admin" {
  bucket = aws_s3_bucket.admin.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "admin" {
  bucket = aws_s3_bucket.admin.id
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_logging" "admin" {
  bucket = aws_s3_bucket.admin.id

  # TODO: consider adding this...
  # expected_bucket_owner = local.account_id

  target_bucket = local.logging_bucket
  target_prefix = local.logging_prefix
}

resource "aws_s3_bucket_acl" "admin" {
  bucket = aws_s3_bucket.admin.id
  acl    = "private"
}

resource "aws_s3_bucket" "logging" {
  count  = local.is_ephemeral_env ? 0 : 1
  bucket = local.logging_bucket
  tags = {
    role = "logs"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "logging" {
  count  = local.is_ephemeral_env ? 0 : 1
  bucket = aws_s3_bucket.logging[0].id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "logging" {
  count  = local.is_ephemeral_env ? 0 : 1
  bucket = aws_s3_bucket.logging[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_acl" "logging" {
  count  = local.is_ephemeral_env ? 0 : 1
  acl    = "log-delivery-write"
  bucket = aws_s3_bucket.logging[0].id
}

resource "aws_s3_bucket_policy" "logging" {
  count  = local.is_ephemeral_env ? 0 : 1
  bucket = aws_s3_bucket.logging[0].id
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
      "Resource": "arn:aws:s3:::bfd-${local.data_env}-logs-${local.account_id}/*"
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
        "arn:aws:s3:::bfd-${local.data_env}-logs-${local.account_id}",
        "arn:aws:s3:::bfd-${local.data_env}-logs-${local.account_id}/*"
      ],
      "Sid": "AllowSSLRequestsOnly"
    }
  ],
  "Version": "2012-10-17"
}
POLICY
}
