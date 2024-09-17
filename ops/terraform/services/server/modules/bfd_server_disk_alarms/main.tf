locals {
  env = terraform.workspace

  account_id = data.aws_caller_identity.current.account_id

  kms_key_arn = data.aws_kms_key.mgmt_cmk.arn
  kms_key_id  = data.aws_kms_key.mgmt_cmk.key_id

  topic_name = "bfd-server-${local.env}-instance-launch-terminate"

  lambda_timeout_seconds = 30
  lambda_name            = "manage-disk-usage-alarms"

  alarms_prefix = "bfd-server-${local.env}-alert-disk-usage-percent"

  alarm_action_sns_by_env = {
    test     = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-test"
    prod-sbx = "bfd-${local.env}-cloudwatch-alarms"
    prod     = "bfd-${local.env}-cloudwatch-alarms"
  }
  alarm_action_sns = try(coalesce(
    var.alarm_action_sns_override,
    lookup(local.alarm_action_sns_by_env, local.env, null)
  ), null)
  alarms_ok_sns = var.alarm_ok_sns_override
}

resource "aws_sns_topic" "this" {
  name              = local.topic_name
  kms_master_key_id = local.kms_key_id
}

resource "aws_autoscaling_notification" "this" {
  topic_arn   = aws_sns_topic.this.arn
  group_names = [var.asg_name]

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
  name        = "bfd-${local.env}-${local.lambda_name}-logs"
  description = "Permissions to create and write to bfd-${local.env}-${local.lambda_name} logs"
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
        "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/lambda/bfd-${local.env}-${local.lambda_name}:*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "kms" {
  name        = "bfd-${local.env}-${local.lambda_name}-kms"
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

resource "aws_iam_policy" "autoscaling" {
  name        = "bfd-${local.env}-${local.lambda_name}-autoscaling"
  description = "Permissions for bfd-${local.env}-${local.lambda_name} to describe ASGs"
  # Unfortunately AWS does not support anything but wildcarding for the resource definition for the
  # DescribeAutoScalingGroups action
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "VisualEditor0",
      "Effect": "Allow",
      "Action": [
        "autoscaling:DescribeAutoScalingGroups"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_policy" "cloudwatch" {
  name        = "bfd-${local.env}-${local.lambda_name}-cloudwatch"
  description = "Permissions for bfd-${local.env}-${local.lambda_name} to create and destroy metric alarms"
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
      "Resource": "arn:aws:cloudwatch:us-east-1:577373831711:alarm:*"
    }
  ]
}
EOF
}

resource "aws_iam_role" "this" {
  name        = "bfd-${local.env}-${local.lambda_name}"
  path        = "/"
  description = "Role for bfd-${local.env}-${local.lambda_name} Lambda"

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
    aws_iam_policy.autoscaling.arn,
    aws_iam_policy.cloudwatch.arn
  ]
}

resource "aws_lambda_function" "this" {
  description = join("", [
    "Creates and destroys per-instance disk usage alarms when launch and terminate notifications ",
    "from AutoScaling Notifications are received"
  ])
  function_name = "bfd-${local.env}-${local.lambda_name}"
  tags          = { Name = "bfd-${local.env}-${local.lambda_name}" }

  filename         = data.archive_file.this.output_path
  source_code_hash = data.archive_file.this.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "manage_disk_usage_alarms.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.11"
  timeout          = local.lambda_timeout_seconds
  environment {
    variables = {
      ENV              = local.env
      ALARM_THRESHOLD  = "95.0"
      ALARM_PERIOD     = "60"
      ALARM_ACTION_ARN = try(data.aws_sns_topic.alarms_action_sns[0].arn, null)
      OK_ACTION_ARN    = try(data.aws_sns_topic.alarms_ok_sns[0].arn, null)
      METRIC_NAMESPACE = "bfd-${local.env}/bfd-server/CWAgent"
      METRIC_NAME      = "disk_used_percent"
      ALARMS_PREFIX    = local.alarms_prefix
    }
  }

  role = aws_iam_role.this.arn
}
