# Zip File containing Lambda script
data "archive_file" "zip-archive-format-firehose-logs" {
  type        = "zip"
  source_file  = "${path.module}/lambda_src/bfd-cw-to-flattened-json.py"
  output_path = "${path.module}/lambda_src/${local.environment}/bfd-cw-to-flattened-json.zip"
}

# Lambda Function to process logs from Firehose
resource "aws_lambda_function" "lambda-function-format-firehose-logs" {
  architectures = [
    "x86_64",
  ]
  description                    = "Extracts and flattens JSON messages from CloudWatch log subscriptions"
  function_name                  = "${local.full_name}-cw-to-flattened-json"
  filename                       = data.archive_file.zip-archive-format-firehose-logs.output_path
  handler                        = "bfd-cw-to-flattened-json.lambda_handler"
  layers                         = []
  memory_size                    = 128
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = aws_iam_role.iam-role-firehose-lambda.arn
  runtime                        = "python3.8"
  source_code_hash               = data.archive_file.zip-archive-format-firehose-logs.output_base64sha256

  tags = { "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python" }
  
  timeout = 300

  ephemeral_storage {
    size = 512
  }

  timeouts {}

  tracing_config {
    mode = "PassThrough"
  }
}
