##
# Original Cloudformation config from ITOPS
##

resource "aws_s3_bucket" "cf-bfd-management-vpc" {
  bucket = "cf-bfd-management-vpc"
}

resource "aws_s3_bucket_public_access_block" "cf-bfd-management-vpc" {
  bucket = aws_s3_bucket.cf-bfd-management-vpc.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cf-bfd-management-vpc" {
  bucket = aws_s3_bucket.cf-bfd-management-vpc.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "cf-bfd-prod-vpc" {
  bucket = "cf-bfd-prod-vpc"
}

resource "aws_s3_bucket_public_access_block" "cf-bfd-prod-vpc" {
  bucket = aws_s3_bucket.cf-bfd-prod-vpc.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cf-bfd-prod-vpc" {
  bucket = aws_s3_bucket.cf-bfd-prod-vpc.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "cf-bfd-prod-sbx-vpc" {
  bucket = "cf-bfd-prod-sbx-vpc"
}

resource "aws_s3_bucket_public_access_block" "cf-bfd-prod-sbx-vpc" {
  bucket = aws_s3_bucket.cf-bfd-prod-sbx-vpc.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cf-bfd-prod-sbx-vpc" {
  bucket = aws_s3_bucket.cf-bfd-prod-sbx-vpc.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "cf-templates-13lbg6e67ga5v-us-east-1" {
  bucket = "cf-templates-13lbg6e67ga5v-us-east-1"
}

resource "aws_s3_bucket_public_access_block" "cf-templates-13lbg6e67ga5v-us-east-1" {
  bucket = aws_s3_bucket.cf-templates-13lbg6e67ga5v-us-east-1.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cf-templates-13lbg6e67ga5v-us-east-1" {
  bucket = aws_s3_bucket.cf-templates-13lbg6e67ga5v-us-east-1.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "bfd-config" {
  bucket = "bfd-config-${local.account_id}"
}

resource "aws_s3_bucket_public_access_block" "bfd-config" {
  bucket = aws_s3_bucket.bfd-config.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "bfd-config" {
  bucket = aws_s3_bucket.bfd-config.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket" "cf-bfd-vpc-template" {
  bucket = "cf-bfd-vpc-template"
}

resource "aws_s3_bucket_public_access_block" "cf-bfd-vpc-template" {
  bucket = aws_s3_bucket.cf-bfd-vpc-template.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cf-bfd-vpc-template" {
  bucket = aws_s3_bucket.cf-bfd-vpc-template.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
