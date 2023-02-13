data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-${local.env}-cmk"
}

data "archive_file" "lambda_src" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/trigger_glue_crawler.py"
  output_path = "${path.module}/lambda_src/trigger_glue_crawler.zip"
}
