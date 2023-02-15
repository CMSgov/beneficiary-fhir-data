data "aws_region" "current" {}

data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-${local.env}-cmk"
}

data "aws_s3_bucket" "etl" {
  bucket = var.etl_bucket_id
}

data "archive_file" "lambda_src" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/update_pipeline_slis.py"
  output_path = "${path.module}/lambda_src/update_pipeline_slis.zip"
}
