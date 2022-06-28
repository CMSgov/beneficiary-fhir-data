data "aws_s3_bucket" "bfd-insights-bucket" {
  bucket = "bfd-insights-bfd-${local.account_id}"
}

data "aws_kms_key" "kms_key" {
  key_id = "alias/bfd-insights-bfd-cmk"
}
