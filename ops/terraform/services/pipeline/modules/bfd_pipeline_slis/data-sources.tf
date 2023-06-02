data "aws_region" "current" {}

data "aws_s3_bucket" "etl" {
  bucket = var.etl_bucket_id
}

data "aws_sns_topic" "this" {
  name = var.s3_events_sns_topic_name
}

data "archive_file" "lambda_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/update_pipeline_slis.zip"

  source {
    content  = file("${path.module}/lambda_src/__init__.py")
    filename = "__init__.py"
  }
  source {
    content  = file("${path.module}/lambda_src/update_pipeline_slis.py")
    filename = "update_pipeline_slis.py"
  }
  source {
    content  = file("${path.module}/lambda_src/backoff_retry.py")
    filename = "backoff_retry.py"
  }
  source {
    content  = file("${path.module}/lambda_src/sqs.py")
    filename = "sqs.py"
  }
  source {
    content  = file("${path.module}/lambda_src/cw_metrics.py")
    filename = "cw_metrics.py"
  }
}
