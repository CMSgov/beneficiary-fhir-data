# IAM

data "aws_caller_identity" "current" {}

data "aws_iam_group" "bfd-analysts" {
  group_name = "bfd-insights-analysts"
}

# data "aws_iam_role" "glue-role" {
#   name = "bfd-insights-bfd-glue-role"
# }

# S3

data "aws_s3_bucket" "bfd-app-logs" {
  bucket = "bfd-insights-bfd-app-logs"
}

data "aws_s3_bucket" "bfd-insights-bucket" {
  bucket = "bfd-insights-bfd-${data.aws_caller_identity.current.account_id}"
}

data "aws_s3_bucket" "bfd-glue-assets" {
  bucket = "aws-glue-assets-577373831711-us-east-1"
}

data "aws_kms_key" "kms_key" {
  key_id = "9bfd6886-7124-4229-931a-4a30ce61c0ea"
}
