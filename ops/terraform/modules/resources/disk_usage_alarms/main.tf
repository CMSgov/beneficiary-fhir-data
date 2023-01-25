locals {
  account_id = data.aws_caller_identity.current.account_id

  lambda_timeout_seconds = 30
  lambda_name            = "manage-disk-usage-alarms"
}

resource "aws_cloudwatch_event_rule" "autoscaling_instance_launch_terminate" {
  name = "bfd-${var.env}-autoscaling-instance-launch-terminate"
  description = "Filters for bfd-server EC2 instance launches and terminations in ${var.env} ASG"
  event_pattern = <<-EOF
{
  "source": ["aws.autoscaling"],
  "detail-type": [
    "EC2 Instance Launch Successful",
    "EC2 Instance Terminate Successful"
  ],
  "detail": {
    "AutoScalingGroupName": [
      {
        "prefix": "bfd-${var.env}-fhir"
      }
    ]
  }
}
EOF
}

resource "aws_cloudwatch_event_target" "invoke_lambda_from_autoscaling_event" {
  arn = aws_lambda_function.this.arn
  rule = aws_cloudwatch_event_rule.autoscaling_instance_launch_terminate.name
}

resource "aws_lambda_permission" "allow_eventbridge_to_invoke_lambda" {
  statement_id = "AllowExecutionFromCloudWatch"
  action = "lambda:InvokeFunction"
  function_name = aws_lambda_function.this.function_name
  principal = "events.amazonaws.com"
  source_arn = aws_cloudwatch_event_rule.autoscaling_instance_launch_terminate.arn
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

  managed_policy_arns = [aws_iam_policy.logs.arn]
}

resource "aws_lambda_function" "this" {
  description = join("", [
    "Creates and destroys per-instance disk usage alarms when launch and terminate events from ",
    "EventBridge are received"
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
    }
  }

  role = aws_iam_role.this.arn
}
