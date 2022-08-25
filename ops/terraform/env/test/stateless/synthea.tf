# Temporary synthea loading resources for BFD-1652.
# It will be safe to discard all resources in this .tf after
# test environment synthetic data load
data "aws_caller_identity" "current" {}

data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-test-cmk"
}

resource "aws_s3_bucket" "synthea" {
  acl           = "private"
  bucket        = "bfd-test-synthea-etl-${data.aws_caller_identity.current.account_id}"
  force_destroy = false
  request_payer = "BucketOwner"
  tags = {
    "Environment" = "test"
    "Layer"       = "data"
    "application" = "bfd"
    "business"    = "oeda"
    "role"        = "etl"
    "stack"       = "test"
  }
  tags_all = {
    "Environment" = "test"
    "Layer"       = "data"
    "application" = "bfd"
    "business"    = "oeda"
    "role"        = "etl"
    "stack"       = "test"
  }

  logging {
    target_bucket = "bfd-test-logs-${data.aws_caller_identity.current.account_id}"
    target_prefix = "etl_s3_access_logs/"
  }

  server_side_encryption_configuration {
    rule {
      bucket_key_enabled = false

      apply_server_side_encryption_by_default {
        kms_master_key_id = data.aws_kms_key.master_key.arn
        sse_algorithm     = "aws:kms"
      }
    }
  }

  versioning {
    enabled    = false
    mfa_delete = false
  }
}

resource "aws_iam_policy" "synthea" {
  description = "Allow the BFD Pipeline application to read-write the S3 bucket with the Synthea RIF in it."
  name        = "bfd-test-synthea-pipeline-rw-s3-rif"
  path        = "/"
  policy = jsonencode(
    {
      Statement = [
        {
          Action = [
            "kms:Encrypt",
            "kms:Decrypt",
            "kms:ReEncrypt*",
            "kms:GenerateDataKey*",
            "kms:DescribeKey",
          ]
          Effect = "Allow"
          Resource = [
            "${data.aws_kms_key.master_key.arn}"
          ]
          Sid = "BFDPipelineRWS3RIFKMS"
        },
        {
          Action = [
            "s3:ListBucket",
          ]
          Effect = "Allow"
          Resource = [
            "${aws_s3_bucket.synthea.arn}"
          ]
          Sid = "BFDPipelineRWS3RIFListBucket"
        },
        {
          Action = [
            "s3:ListBucket",
            "s3:GetBucketLocation",
            "s3:GetObject",
            "s3:PutObject",
            "s3:DeleteObject",
          ]
          Effect = "Allow"
          Resource = [
            "${aws_s3_bucket.synthea.arn}/*"
          ]
          Sid = "BFDPipelineRWS3RIFReadWriteObjects"
        },
      ]
      Version = "2012-10-17"
    }
  )
}

resource "aws_s3_bucket_public_access_block" "synthea" {
  bucket = aws_s3_bucket.synthea.id

  block_public_acls   = true
  block_public_policy = true
}

resource "aws_iam_role_policy_attachment" "synthea" {
  policy_arn = aws_iam_policy.synthea.arn
  role       = "bfd-test-bfd_pipeline-role"
}
