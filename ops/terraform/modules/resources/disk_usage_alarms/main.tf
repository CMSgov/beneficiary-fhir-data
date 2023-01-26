locals {
  account_id = data.aws_caller_identity.current.account_id

  kms_key_arn = data.aws_kms_key.mgmt_cmk.arn
  kms_key_id  = data.aws_kms_key.mgmt_cmk.key_id

  topic_name = "bfd-server-${var.env}-instance-launch-terminate"

  lambda_timeout_seconds = 30
  lambda_name            = "manage-disk-usage-alarms"

  alarms_prefix = "bfd-server-${var.env}-alert-disk-usage-percent"
}

# TODO: This SNS topic (and related resources below) mostly duplicates a similar topic used for
# server-load; consolidate these in the future
resource "aws_sns_topic" "this" {
  name              = local.topic_name
  kms_master_key_id = local.kms_key_id
}

resource "aws_autoscaling_notification" "this" {
  topic_arn = aws_sns_topic.this.arn

  group_names = [
    data.aws_autoscaling_group.asg.name,
  ]

  notifications = [
    "autoscaling:EC2_INSTANCE_LAUNCH",
    "autoscaling:EC2_INSTANCE_TERMINATE"
  ]
}

resource "aws_lambda_permission" "this" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.this.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.this.arn
}

resource "aws_sns_topic_subscription" "this" {
  topic_arn = aws_sns_topic.this.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.this.arn
}

resource "aws_iam_policy" "logs" {
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
      "Action": ["logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": [
        "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/lambda/bfd-${var.env}-${local.lambda_name}:*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "kms" {
  name        = "bfd-${var.env}-${local.lambda_name}-kms"
  description = "Permissions to decrypt mgmt KMS key"
  policy      = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["kms:Decrypt"],
      "Resource": ["${local.kms_key_arn}"]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "cloudwatch" {
  name        = "bfd-${var.env}-${local.lambda_name}-cloudwatch"
  description = "Permissions for bfd-${var.env}-${local.lambda_name} to create and destroy metric alarms"
  policy      = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "VisualEditor0",
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricAlarm",
        "cloudwatch:DeleteAlarms",
        "cloudwatch:DescribeAlarms"
      ],
      "Resource": "arn:aws:cloudwatch:us-east-1:577373831711:alarm:${local.alarms_prefix}*"
    }
  ]
}
EOF
}

resource "aws_iam_role" "this" {
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
    aws_iam_policy.logs.arn,
    aws_iam_policy.kms.arn,
    aws_iam_policy.cloudwatch.arn
  ]
}

resource "aws_lambda_function" "this" {
  description = join("", [
    "Creates and destroys per-instance disk usage alarms when launch and terminate notifications ",
    "from AutoScaling Notifications are received"
  ])
  function_name = "bfd-${var.env}-${local.lambda_name}"
  tags          = { Name = "bfd-${var.env}-${local.lambda_name}" }

  filename         = data.archive_file.this.output_path
  source_code_hash = data.archive_file.this.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "manage_disk_usage_alarms.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = local.lambda_timeout_seconds
  environment {
    variables = {
      ENV              = var.env
      ALARM_THRESHOLD  = "95.0"
      ALARM_PERIOD     = "60"
      ALARM_ACTION_ARN = data.aws_sns_topic.cloudwatch_alarms_alert.arn
      OK_ACTION_ARN    = data.aws_sns_topic.cloudwatch_alarms_ok.arn
      METRIC_NAMESPACE = "bfd-${var.env}/bfd-server/CWAgent"
      METRIC_NAME      = "disk_used_percent"
      ALARMS_PREFIX    = local.alarms_prefix
    }
  }

  role = aws_iam_role.this.arn
}
