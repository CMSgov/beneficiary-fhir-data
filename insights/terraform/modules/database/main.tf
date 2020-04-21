data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "moderate_bucket" {
  bucket      = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

data "aws_kms_alias" "s3" {
  name = "alias/aws/s3"
}

locals {
  account_id  = data.aws_caller_identity.current.account_id
  bucket_arn  = data.aws_s3_bucket.moderate_bucket.arn
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
      s3_encryption_mode = "SSE-S3"
    }
  }
}