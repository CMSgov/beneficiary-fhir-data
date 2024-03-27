data "aws_region" "current" {}

data "aws_ssm_parameters_by_path" "nonsensitive_service" {
  path = "/bfd/${local.env}/${local.service}/${local.variant}/nonsensitive"
}

data "aws_ssm_parameters_by_path" "nonsensitive_ccw" {
  path = "/bfd/${local.env}/${local.service}/nonsensitive/ccw"
}

data "aws_s3_bucket" "etl" {
  bucket = var.etl_bucket_id
}

data "aws_sns_topic" "this" {
  name = var.s3_events_sns_topic_name
}

data "archive_file" "lambda_src" {
  for_each = local.lambdas

  type        = "zip"
  output_path = "${path.module}/lambda_src/${each.value.src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${each.value.src}.py")
    filename = "${each.value.src}.py"
  }

  source {
    content  = file("${path.module}/lambda_src/__init__.py")
    filename = "__init__.py"
  }

  source {
    content  = file("${path.module}/lambda_src/common.py")
    filename = "common.py"
  }

  source {
    content  = file("${path.module}/lambda_src/backoff_retry.py")
    filename = "backoff_retry.py"
  }

  source {
    content  = file("${path.module}/lambda_src/dynamo_db.py")
    filename = "dynamo_db.py"
  }

  source {
    content  = file("${path.module}/lambda_src/cw_metrics.py")
    filename = "cw_metrics.py"
  }
}
