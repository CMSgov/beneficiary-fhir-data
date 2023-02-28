locals {
  account_id = data.aws_caller_identity.current.account_id

  lambda_timeout_seconds = 30
  lambda_name            = "cloudwatch-alarms-slack-notifier"

  kms_key_arn = data.aws_kms_key.mgmt_cmk.arn
  kms_key_id  = data.aws_kms_key.mgmt_cmk.key_id

  lambda_configs_by_channel = {
    bfd_notices = {
      sns_topic        = data.aws_sns_topic.cloudwatch_alarms_slack_bfd_notices
      webhook_ssm_path = "/bfd/mgmt/common/sensitive/slack_webhook_bfd_notices"
    }
    bfd_test = {
      sns_topic        = data.aws_sns_topic.cloudwatch_alarms_slack_bfd_test
      webhook_ssm_path = "/bfd/mgmt/common/sensitive/slack_webhook_bfd_test"
    }
    bfd_warnings = {
      sns_topic        = data.aws_sns_topic.cloudwatch_alarms_slack_bfd_warnings
      webhook_ssm_path = "/bfd/mgmt/common/sensitive/slack_webhook_bfd_warnings"
    }
    bfd_alerts = {
      sns_topic        = data.aws_sns_topic.cloudwatch_alarms_slack_bfd_alerts
      webhook_ssm_path = "/bfd/mgmt/common/sensitive/slack_webhook_bfd_alerts"
    }
  }

  lambdas = {
    for k, v in local.lambda_configs_by_channel :
    k => merge(v, { full_name = "${local.lambda_name}-${replace(k, "_", "-")}" })
  }
}

resource "aws_iam_policy" "logs" {
  for_each = local.lambdas

  name        = "bfd-${var.env}-${each.value.full_name}-logs"
  description = "Permissions to create and write to bfd-${var.env}-${each.value.full_name} logs"
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
                "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/lambda/bfd-${var.env}-${each.value.full_name}:*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "kms" {
  for_each = local.lambdas

  name        = "bfd-${var.env}-${each.value.full_name}-kms"
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

resource "aws_iam_policy" "ssm" {
  for_each = local.lambdas

  name        = "bfd-${var.env}-${each.value.full_name}-ssm-parameters"
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

resource "aws_iam_role" "this" {
  for_each = local.lambdas

  name        = "bfd-${var.env}-${each.value.full_name}"
  path        = "/"
  description = "Role for bfd-${var.env}-${each.value.full_name} Lambda"

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
    aws_iam_policy.logs[each.key].arn,
    aws_iam_policy.kms[each.key].arn,
    aws_iam_policy.ssm[each.key].arn
  ]
}

resource "aws_lambda_permission" "this" {
  for_each = local.lambdas

  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.this[each.key].function_name
  principal     = "sns.amazonaws.com"
  source_arn    = each.value.sns_topic.arn
}

resource "aws_sns_topic_subscription" "this" {
  for_each = local.lambdas

  topic_arn = each.value.sns_topic.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.this[each.key].arn
}

resource "aws_lambda_function" "this" {
  for_each = local.lambdas

  description   = "Sends a Slack notification whenever an SNS notification is received from a CloudWatch Alarm"
  function_name = "bfd-${var.env}-${each.value.full_name}"
  tags          = { Name = "bfd-${var.env}-${each.value.full_name}" }

  filename         = data.archive_file.this.output_path
  source_code_hash = data.archive_file.this.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "cw_alarms_slack_notifier.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = local.lambda_timeout_seconds
  environment {
    variables = {
      ENV              = var.env
      WEBHOOK_SSM_PATH = each.value.webhook_ssm_path
    }
  }

  role = aws_iam_role.this[each.key].arn
}
