provider "aws" {
  region = "us-east-1"
}

resource "aws_kms_key" "state_kms_key" {
  description             = "bfd-tf-state"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}

resource "aws_kms_alias" "state_kms_alias" {
  name          = "alias/bfd-tf-state"
  target_key_id = "${aws_kms_key.state_kms_key.key_id}"
}

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
            "Resource": "arn:aws:s3:::bfd-tf-state/*",
## CHANGE ME -jz
#            "Condition": {
#                "ArnEquals": {
#                    "aws:userid": "arn:aws:iam::577373831711:user/Jenkins"
#                }
#            }
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

resource "aws_dynamodb_table" "state_table" {
  name           = "bfd-tf-table"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }
}
