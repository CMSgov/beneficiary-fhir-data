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

locals {
  account_id = data.aws_caller_identity.current.account_id

  lambda_timeout_seconds = 30
  lambda_name            = "slack-alarms-notifier"

  kms_key_arn = data.aws_kms_key.mgmt_cmk.arn
  kms_key_id  = data.aws_kms_key.mgmt_cmk.key_id
}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "mgmt_cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}

data "aws_sns_topic" "cloudwatch_alarms_alert" {
  # TODO: Replace this with the non-temporary variant of the CloudWatch alarms SNS topic
  name = "bfd-${var.env}-cloudwatch-alarms-alert-testing"
}

data "archive_file" "slack_alarms_notifier" {
  type        = "zip"
  source_file = "${path.module}/lambda-src/slack-alarms-notifier/slack-alarms-notifier.py"
  output_path = "${path.module}/lambda-src/slack-alarms-notifier/slack-alarms-notifier.zip"
}

resource "aws_iam_policy" "logs_slack_alarms_notifier" {
  name        = "bfd-${var.env}-${local.lambda_name}-logs"
  description = "Permissions to create and write to bfd-${var.env}-${local.lambda_name} logs"
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
                "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/lambda/bfd-${var.env}-${local.lambda_name}:*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "kms_slack_alarms_notifier" {
  name        = "bfd-${var.env}-${local.lambda_name}-kms"
  description = "Permissions to decrypt mgmt KMS key"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "kms:Decrypt"
            ],
            "Resource": [
                "${local.kms_key_arn}"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "ssm_slack_alarms_notifier" {
  name        = "bfd-${var.env}-${local.lambda_name}-ssm-parameters"
  description = "Permissions to /bfd/mgmt/common/sensitive/* SSM hierarchies"
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

resource "aws_iam_role" "slack_alarms_notifier" {
  name        = "bfd-${var.env}-${local.lambda_name}"
  path        = "/"
  description = "Role for bfd-${var.env}-${local.lambda_name} Lambda"

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
    aws_iam_policy.logs_slack_alarms_notifier.arn,
    aws_iam_policy.kms_slack_alarms_notifier.arn,
    aws_iam_policy.ssm_slack_alarms_notifier.arn
  ]
}

resource "aws_lambda_permission" "bfd_insights_error_slack" {
  statement_id   = "bfd-${local.env}-${local.service}-bfd-insights-error-slack-alert"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.bfd_insights_error_slack.arn
  principal      = "s3.amazonaws.com"
  source_arn     = data.aws_s3_bucket.insights.arn
  source_account = local.account_id
}

resource "aws_lambda_function" "slack_alarms_notifier" {
  description   = "Sends a Slack notification whenever an SNS notification is received from SLO alarms"
  function_name = "bfd-${var.env}-${local.lambda_name}"
  tags          = { Name = "bfd-${var.env}-${local.lambda_name}" }

  filename         = data.archive_file.slack_alarms_notifier.output_path
  source_code_hash = data.archive_file.slack_alarms_notifier.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "slack-alarms-notifier.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = local.lambda_timeout_seconds
  environment {
    variables = {
      ENV = var.env
    }
  }

  role = aws_iam_role.slack_alarms_notifier.arn
}
