data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "main" {
  bucket      = var.bucket
}

data "aws_kms_key" "bucket_cmk" {
  key_id      = var.bucket_cmk
}

locals {
  account_id  = data.aws_caller_identity.current.account_id
  bucket_arn  = data.aws_s3_bucket.main.arn
}

resource "aws_glue_catalog_database" "main" {
  name        = var.database
  description = "BFD Insights database"
}

resource "aws_glue_security_configuration" "main" {
  name        = var.database

  encryption_configuration {
    cloudwatch_encryption {
      cloudwatch_encryption_mode = "DISABLED"
    }

    job_bookmarks_encryption {
      job_bookmarks_encryption_mode = "DISABLED"
    }

    s3_encryption {
      kms_key_arn        = data.aws_kms_key.bucket_cmk.arn
      s3_encryption_mode = "SSE-KMS"
    }
  }
}