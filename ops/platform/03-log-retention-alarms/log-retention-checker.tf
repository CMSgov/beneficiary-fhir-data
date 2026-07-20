data "archive_file" "checker_lambda_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.checker_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.checker_lambda_src}.py")
    filename = "${local.checker_lambda_src}.py"
  }
}

data "aws_sns_topic" "checker_alert_topic" {
  count = local.checker_alert_topic_name != null && local.checker_alert_topic_name != "UNDEFINED" ? 1 : 0
  name  = local.checker_alert_topic_name
}

resource "aws_scheduler_schedule_group" "checker" {
  name = "${local.checker_lambda_full_name}-schedules"
}

resource "aws_scheduler_schedule" "checker" {
  group_name          = aws_scheduler_schedule_group.checker.name
  name                = "${local.checker_lambda_abbv_name}-every-${replace(local.checker_lambda_rate, " ", "-")}"
  schedule_expression = "rate(${local.checker_lambda_rate})"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_lambda_function.checker.arn
    role_arn = aws_iam_role.scheduler_assume_role.arn
  }
}

resource "aws_cloudwatch_log_group" "checker" {
  name              = "/aws/lambda/${local.checker_lambda_full_name}"
  retention_in_days = local.required_retention_in_days
  kms_key_id        = local.env_key_arn
  skip_destroy      = true
}

resource "aws_lambda_function" "checker" {
  function_name = local.checker_lambda_full_name

  description = join("", [
    "Invoked on EventBridge schedules, this Lambda checks CloudWatch Log Group retention ",
    "settings and alerts when retention does not match the configured policy"
  ])

  tags = {
    Name = local.checker_lambda_full_name
  }

  kms_key_arn      = local.env_key_arn
  filename         = data.archive_file.checker_lambda_src.output_path
  source_code_hash = data.archive_file.checker_lambda_src.output_base64sha256
  architectures    = ["arm64"]
  handler          = "${local.checker_lambda_src}.lambda_handler"
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python314-arm64:27"]
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.14"
  timeout          = 300

  environment {
    variables = {
      REQUIRED_RETENTION_DAYS = local.required_retention_in_days
      ALERT_SNS_TOPIC_ARN     = length(data.aws_sns_topic.checker_alert_topic) > 0 ? one(data.aws_sns_topic.checker_alert_topic[*].arn) : ""
    }
  }

  role = aws_iam_role.checker.arn
}
