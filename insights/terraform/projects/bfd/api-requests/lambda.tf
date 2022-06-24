# Zip File containing Lambda script
data "archive_file" "bfd-cw-to-flattened" {
  type        = "zip"
  source_dir  = "${path.module}/lambda_src/"
  output_path = "${path.module}/lambda_src/${local.environment}/bfd-cw-to-flattened-json.zip"
}

# Lambda Function to process logs from Firehose
resource "aws_lambda_function" "bfd-cw-to-flattened-json" {
  architectures = [
    "x86_64",
  ]
  description                    = "Extracts and flattens JSON messages from CloudWatch log subscriptions"
  function_name                  = "${local.full_name}-cw-to-flattened-json"
  filename                       = data.archive_file.bfd-cw-to-flattened.output_path
  handler                        = "bfd-cw-to-flattened-json.lambda_handler"
  layers                         = []
  memory_size                    = 128
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = aws_iam_role.firehose-lambda-role.arn
  runtime                        = "python3.8"
  tags = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python"
  }
  tags_all = {
    "lambda-console:blueprint" = "kinesis-firehose-cloudwatch-logs-processor-python"
  }
  timeout = 300

  ephemeral_storage {
    size = 512
  }

  timeouts {}

  tracing_config {
    mode = "PassThrough"
  }
}
