## Terraform State and Build Buckets
#
provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

data "aws_kms_key" "tf_state" {
  key_id = "alias/bfd-tf-state"
}


## TF State Bucket
#
resource "aws_s3_bucket" "state_bucket" {
  bucket = var.bfd_tf_state_bucket
  acl    = "private"

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
            "Resource": "arn:aws:s3:::${var.bfd_tf_state_bucket}/*",
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
            "Resource": "arn:aws:s3:::${var.bfd_tf_state_bucket}/*",
            "Condition": {
                "ArnEquals": {
                    "aws:userid": "arn:aws:iam::${var.bcda_acct_num}:user/Jenkins"
                }
                "StringNotEquals": {
                    "s3:x-amz-server-side-encryption": "aws:kms"
                }
            }
        }
    ]
}
EOF

  versioning {
    enabled = true
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = data.aws_kms_key.tf_state.id
        sse_algorithm     = "aws:kms"
      }
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}


## Bucket for Packages, RPM, WAR, JAR, etc.
#
resource "aws_s3_bucket" "bfd_packages_bucket" {
  bucket = var.bfd_packages_bucket
  acl    = "private"

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
            "Resource": "arn:aws:s3:::${var.bfd_packages_bucket}/*",
        },
        {
            "Sid": "JenkinsGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::${var.bfd_packages_bucket}/*",
            "Condition": {
                "ArnEquals": {
                    "aws:userid": "arn:aws:iam::${var.bcda_acct_num}:user/Jenkins"
                }
            }
        }
    ]
}
EOF

  versioning {
    enabled = true
  }

  lifecycle {
    prevent_destroy = true
  }
}
