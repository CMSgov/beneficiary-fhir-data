provider "aws" {
  region = "us-east-1"
}

/* Terraform State Bucket */
resource "aws_s3_bucket" "state_bucket" {
  bucket = "bfd-tf-state"
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
            "Resource": "arn:aws:s3:::bfd-tf-state/*",

        },
        {
            "Sid": "JenkinsGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::bfd-tf-state/*",
            "Condition": {
                "ArnEquals": {
                    "aws:userid": "arn:aws:iam::755619740999:user/Jenkins"
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
        kms_master_key_id = "${aws_kms_key.state_kms_key.arn}"
        sse_algorithm     = "aws:kms"
      }
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}

/* Bucket for Packages, RPM, WAR, JAR, etc. */

resource "aws_s3_bucket" "state_bucket" {
  bucket = "bfd-packages"
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
            "Resource": "arn:aws:s3:::bfd-packages/*",
        },
        {
            "Sid": "JenkinsGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::bfd-packages/*",
            "Condition": {
                "ArnEquals": {
                    "aws:userid": "arn:aws:iam::755619740999:user/Jenkins"
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
