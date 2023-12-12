locals {
  bfd_state_bucket        = "bfd-tf-state"
  bcda_aws_account_number = data.aws_ssm_parameter.bcda_aws_account_number.value
  state_kms_key_id        = data.aws_kms_key.state.arn
  legacy_kms_key_id       = data.aws_kms_key.legacy.arn
  cloudtrail_logs_bucket  = "bfd-cloudtrail-logs"

  default_tags = {
    Environment    = "mgmt"
    application    = "bfd"
    business       = "oeda"
    stack          = "mgmt"
    Terraform      = true
    tf_module_root = "ops/terraform/env/mgmt"
  }
}

data "aws_ssm_parameter" "bcda_aws_account_number" {
  name            = "/bcda/global/terraform/sensitive/aws_account_number"
  with_decryption = true
}

data "aws_kms_key" "state" {
  key_id = "alias/bfd-tf-state"
}

data "aws_kms_key" "legacy" {
  # TODO: Add alias/legacy_kms_key for this key and update here
  key_id = "alias/cloudformation_kms_key"
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
      kms_master_key_id = local.state_kms_key_id
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


resource "aws_s3_bucket" "cloudtrail_logs" {
  bucket = local.cloudtrail_logs_bucket

  # TODO: This is an untagged resource in AWS as of 2022-09-29
  # tags = {
  #   role  = TODO
  #   Layer = TODO
  # }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudtrail_logs" {
  bucket = aws_s3_bucket.cloudtrail_logs.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = local.legacy_kms_key_id
    }
  }
}


resource "aws_s3_bucket_versioning" "cloudtrail_logs" {
  bucket = aws_s3_bucket.cloudtrail_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "cloudtrail_logs" {
  bucket = aws_s3_bucket.cloudtrail_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# TODO: Address in subsequent task/story. Unsure of which bucket, target prefix
# resource "aws_s3_bucket_logging" "admin" {
#   bucket        = aws_s3_bucket.cloudtrail_logs.id
#   target_bucket = TODO
#   target_prefix = TODO
# }

resource "aws_s3_bucket_acl" "cloudtrail_logs" {
  acl    = "private"
  bucket = aws_s3_bucket.cloudtrail_logs.id
}

resource "aws_s3_bucket_policy" "cloudtrail_logs" {
  bucket = aws_s3_bucket.cloudtrail_logs.id
  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AWSCloudTrailAclCheck20150319",
            "Effect": "Allow",
            "Principal": {
                "Service": "cloudtrail.amazonaws.com"
            },
            "Action": "s3:GetBucketAcl",
            "Resource": "${aws_s3_bucket.cloudtrail_logs.arn}"
        },
        {
            "Sid": "AWSCloudTrailWrite20150319",
            "Effect": "Allow",
            "Principal": {
                "Service": "cloudtrail.amazonaws.com"
            },
            "Action": "s3:PutObject",
            "Resource": "${aws_s3_bucket.cloudtrail_logs.arn}/AWSLogs/${local.account_id}/*",
            "Condition": {
                "StringEquals": {
                    "s3:x-amz-acl": "bucket-owner-full-control"
                }
            }
        },
        {
            "Sid": "AllowSSLRequestsOnly",
            "Effect": "Deny",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": [
                "${aws_s3_bucket.cloudtrail_logs.arn}",
                "${aws_s3_bucket.cloudtrail_logs.arn}/*"
            ],
            "Condition": {
                "Bool": {
                    "aws:SecureTransport": "false"
                }
            }
        }
    ]
}
POLICY
}
