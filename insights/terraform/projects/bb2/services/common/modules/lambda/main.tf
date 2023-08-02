data "aws_caller_identity" "current" {}

data "aws_kms_key" "bucket_cmk" {
  key_id = "alias/bfd-insights-bb2-cmk"
}

# Zip File containing Lambda script
data "archive_file" "zip_archive_format_firehose_logs" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/lambda_function.py"
  output_path = "${path.module}/lambda_function.zip"
}


locals {
  account_id               = data.aws_caller_identity.current.account_id
  lambda_role_arn          = "arn:aws:iam::${local.account_id}:role/service-role/${var.role}"
  lambda_function_name_arn = "arn:aws:lambda:${var.region}:${local.account_id}:function:${var.name}"
}

# Lambda Function to process logs from Firehose
resource "aws_lambda_function" "firehose_log_processor_lambda" {
  function_name = local.lambda_function_name_arn


  architectures = [
    "x86_64",
  ]
  description                    = var.description
  filename                       = data.archive_file.zip_archive_format_firehose_logs.output_path
  handler                        = "lambda_function.lambda_handler"
  layers                         = []
  memory_size                    = 128
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = local.lambda_role_arn
  publish                        = false
  runtime                        = "python3.8"
  source_code_hash               = data.archive_file.zip_archive_format_firehose_logs.output_base64sha256

  tags = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python"
  }

  timeout = 60

  timeouts {}

  tracing_config {
    mode = "PassThrough"
  }
}
