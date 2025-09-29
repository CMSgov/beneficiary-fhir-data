locals {
  region     = data.aws_region.current.region
  account_id = data.aws_caller_identity.current.account_id

  lambda_filename = "trigger_glue_crawler"
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "archive_file" "this" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.lambda_filename}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.lambda_filename}.py")
    filename = "${local.lambda_filename}.py"
  }
}

resource "aws_cloudwatch_log_group" "this" {
  name         = "/aws/lambda/${var.lambda_name}"
  kms_key_id   = var.kms_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "this" {
  function_name = var.lambda_name
  description   = "Triggers the ${var.crawler_name} Glue Crawler when any of the ${join(", ", var.partitions)} partitions change"
  tags          = { Name = var.lambda_name }
  kms_key_arn   = var.kms_key_arn

  filename         = data.archive_file.this.output_path
  source_code_hash = data.archive_file.this.output_base64sha256
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python313-arm64:7"]
  architectures    = ["arm64"]
  handler          = "${local.lambda_filename}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.13"
  timeout          = 300

  environment {
    variables = {
      CRAWLER_NAME  = var.crawler_name
      DATABASE_NAME = var.database_name
      TABLE_NAME    = var.table_name
      PARTITIONS    = join(",", var.partitions)
    }
  }

  role = aws_iam_role.this.arn
}
