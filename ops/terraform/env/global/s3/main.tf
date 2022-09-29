locals {
  bfd_state_bucket = "bfd-tf-state"
  bcda_aws_account_number = data.aws_ssm_parameter.bcda_aws_account_number.value
  kms_key_id          = data.aws_kms_key.state.arn
}

provider "aws" {
  version = "~> 4"
  region  = "us-east-1"
}

data "aws_ssm_parameter" "bcda_aws_account_number" {
  name            = "/bcda/global/terraform/sensitive/aws_account_number"
  with_decryption = true
}

data "aws_kms_key" "state" {
  key_id = "alias/bfd-tf-state"
}

resource "aws_s3_bucket" "state" {
  bucket = local.bfd_state_bucket

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_policy" "state" {
  bucket = aws_s3_bucket.state.id
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Id": "PutObjPolicy",
    "Statement": [
        {
            "Sid": "DenyUnEncryptedObjectUploads",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:PutObject",
            "Resource": "arn:aws:s3:::${local.bfd_state_bucket}/*",
            "Condition": {
              "StringNotEquals": {
                "s3:x-amz-server-side-encryption": "aws:kms"
              }
            }
        },
        {
            "Sid": "JenkinsGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::${local.bfd_state_bucket}/*",
            "Condition": {
                "ArnEquals": {
                    "aws:userid": "arn:aws:iam::${local.bcda_aws_account_number}:user/Jenkins"
                }
            }
        },
        {
            "Sid": "AllowSSLRequestsOnly",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": [
                "arn:aws:s3:::bfd-tf-state",
                "arn:aws:s3:::bfd-tf-state/*"
            ],
            "Condition": {
                "Bool": {
                    "aws:SecureTransport": "false"
                }
            }
        }
    ]
}
EOF
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = local.kms_key_id
    }
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_acl" "state" {
  bucket = aws_s3_bucket.state.id
  acl    = "private"
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket = aws_s3_bucket.state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
