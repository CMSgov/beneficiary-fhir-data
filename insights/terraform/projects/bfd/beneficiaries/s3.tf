data "aws_s3_bucket" "bfd-insights-bucket" {
  bucket = "bfd-insights-bfd-${local.account_id}"
}

data "aws_s3_bucket" "bfd-glue-assets" {
  bucket = "aws-glue-assets-${local.account_id}-us-east-1"
}

data "aws_kms_key" "kms_key" {
  key_id = "9bfd6886-7124-4229-931a-4a30ce61c0ea"
}
