data "aws_region" "current" {}

data "aws_kms_key" "env_cmk" {
  key_id = var.env_kms_key_id
}

data "aws_s3_bucket" "etl" {
  bucket = var.etl_bucket_id
}

data "aws_sns_topic" "this" {
  name = var.s3_events_sns_topic_name
}

data "archive_file" "lambda_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/bfd_pipeline_scheduler.zip"

  source {
    content  = file("${path.module}/lambda_src/pipeline_scheduler.py")
    filename = "pipeline_scheduler.py"
  }
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
