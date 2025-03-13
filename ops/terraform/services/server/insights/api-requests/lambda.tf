# Zip File containing Lambda script
data "archive_file" "zip-archive-format-firehose-logs" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/bfd-cw-to-flattened-json.py"
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
  memory_size                    = 256
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = aws_iam_role.iam-role-firehose-lambda.arn
  runtime                        = "python3.11"
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

locals {
  lambda_timeout_seconds = 30
  lambda_name            = "bfd-insights-error-slack"

  mgmt_kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.mgmt_cmk.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )
}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-config-cmk"
}

data "archive_file" "bfd_insights_error_slack" {
  type        = "zip"
  source_file = "${path.module}/lambda_src/bfd-insights-error-slack.py"
  output_path = "${path.module}/lambda_src/bfd-insights-error-slack.zip"
}

resource "aws_iam_policy" "logs_bfd_insights_error_slack" {
  name        = "bfd-${local.environment}-${local.lambda_name}-logs"
  description = "Permissions to create and write to bfd-${local.environment}-${local.lambda_name} logs"
  path = local.cloudtamer_iam_path
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "logs:CreateLogGroup",
            "Resource": "arn:aws:logs:us-east-1:${local.account_id}:*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": [
                "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/lambda/bfd-${local.environment}-${local.lambda_name}:*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "kms_bfd_insights_error_slack" {
  name        = "bfd-${local.environment}-${local.lambda_name}-kms"
  description = "Permissions to decrypt mgmt KMS key"
  path = local.cloudtamer_iam_path
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Effect" : "Allow",
          "Action" : [
            "kms:Decrypt"
          ],
          "Resource" : local.mgmt_kms_config_key_arns
        }
      ]
    }
  )
}

resource "aws_iam_policy" "ssm_bfd_insights_error_slack" {
  name        = "bfd-${local.environment}-${local.lambda_name}-ssm-parameters"
  description = "Permissions to /bfd/mgmt/common/sensitive/* SSM hierarchies"
  path = local.cloudtamer_iam_path
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ssm:GetParametersByPath",
                "ssm:GetParameters",
                "ssm:GetParameter"
            ],
            "Resource": [
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/mgmt/common/sensitive/*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_role" "bfd_insights_error_slack" {
  name        = "bfd-${local.environment}-${local.lambda_name}"
  path        = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  description = "Role for bfd-${local.environment}-${local.lambda_name} Lambda"

  assume_role_policy = <<-EOF
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Action": "sts:AssumeRole",
              "Effect": "Allow",
              "Principal": {
                  "Service": "lambda.amazonaws.com"
              }
          }
      ]
  }
  EOF

  managed_policy_arns = [
    aws_iam_policy.logs_bfd_insights_error_slack.arn,
    aws_iam_policy.kms_bfd_insights_error_slack.arn,
    aws_iam_policy.ssm_bfd_insights_error_slack.arn
  ]
}

resource "aws_lambda_permission" "bfd_insights_error_slack" {
  statement_id   = "bfd-${local.environment}-${local.lambda_name}-bfd-insights-error-slack-alert"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.bfd_insights_error_slack.arn
  principal      = "s3.amazonaws.com"
  source_arn     = data.aws_s3_bucket.bfd-insights-bucket.arn
  source_account = local.account_id
}

resource "aws_lambda_function" "bfd_insights_error_slack" {
  description   = "Sends a Slack notification whenever a new file added to BFD Insights Error Folder"
  function_name = "bfd-${local.environment}-${local.lambda_name}"
  tags          = { Name = "bfd-${local.environment}-${local.lambda_name}" }

  filename         = data.archive_file.bfd_insights_error_slack.output_path
  source_code_hash = data.archive_file.bfd_insights_error_slack.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "bfd-insights-error-slack.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.11"
  timeout          = local.lambda_timeout_seconds
  environment {
    variables = {
      ENV = local.environment
    }
  }

  role = aws_iam_role.bfd_insights_error_slack.arn
}
