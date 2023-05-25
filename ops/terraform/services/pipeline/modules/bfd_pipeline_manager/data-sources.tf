data "aws_region" "current" {}

data "aws_kms_key" "env_cmk" {
  key_id = var.env_kms_key_id
}

data "aws_s3_bucket" "etl" {
  bucket = var.etl_bucket_id
}

data "archive_file" "lambda_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/bfd_pipeline_manager.zip"

  source {
    content  = file("${path.module}/lambda_src/pipeline_manager.py")
    filename = "pipeline_manager.py"
  }
}
