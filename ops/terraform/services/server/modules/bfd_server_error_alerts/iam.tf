resource "aws_iam_policy" "logs" {
  for_each = toset(["${local.alert_lambda_scheduler_name}", "${local.alerting_lambda_name}"])

  name = "${each.key}-logs"
  description = join("", [
    "Permissions for the ${each.key} Lambda to write to its corresponding CloudWatch Log Group ",
    "and Log Stream"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "logs:CreateLogGroup"
          Resource = "arn:aws:logs:${local.region}:${local.account_id}:*"
        },
        {
          Effect = "Allow"
          Action = ["logs:CreateLogStream", "logs:PutLogEvents"]
          Resource = [
            "arn:aws:logs:${local.region}:${local.account_id}:log-group:/aws/lambda/${each.key}:*"
          ]
        }
      ]
    }
  )
}

resource "aws_iam_policy" "scheduler" {
  name = "${local.alert_lambda_scheduler_name}-scheduler"
  description = join("", [
    "Permissions for ${local.alert_lambda_scheduler_name} to create and delete schedules within ",
    "the ${aws_scheduler_schedule_group.this.name} schedule group, list all schedules, and pass ",
    "the ${aws_iam_role.scheduler_assume_role.name} role to EventBridge Scheduler"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = ["scheduler:CreateSchedule", "scheduler:DeleteSchedule"]
          Resource = ["arn:aws:scheduler:${local.region}:${local.account_id}:schedule/${aws_scheduler_schedule_group.this.name}/*"]
        },
        # Unfortunately, ListSchedules does not support any resource-level restrictions
        # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_amazoneventbridgescheduler.html#amazoneventbridgescheduler-schedule
        {
          Effect   = "Allow"
          Action   = ["scheduler:ListSchedules"]
          Resource = ["*"]
        },
        {
          Effect   = "Allow"
          Action   = ["iam:PassRole"]
          Resource = [aws_iam_role.scheduler_assume_role.arn]
        }
      ]
    }
  )
}

resource "aws_iam_policy" "read_log" {
  name = "${local.alerting_lambda_name}-read-logs"
  description = join("", [
    "Permissions for ${local.alerting_lambda_name} to start and retrieve the results of a Log ",
    "Insights query against the ${local.access_json_log_group_name} CloudWatch Log Group"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = ["logs:StartQuery"]
          Resource = [
            "arn:aws:logs:${local.region}:${local.account_id}:log-group:${local.access_json_log_group_name}",
            "arn:aws:logs:${local.region}:${local.account_id}:log-group:${local.access_json_log_group_name}:log-stream:*",
          ]
        },
        # GetQueryResults does not support resource-level restrictions.
        # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_amazoncloudwatchlogs.html
        {
          Effect   = "Allow"
          Action   = ["logs:GetQueryResults"]
          Resource = ["*"]
        }
      ]
    }
  )
}


resource "aws_iam_policy" "invoke_alerter" {
  name = "${local.name_prefix}-scheduler-assumee-allow-lambda-invoke"
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.alerting_lambda_name} Lambda"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "lambda:InvokeFunction"
          Resource = aws_lambda_function.alerting_lambda.arn
        }
      ]
    }
  )
}


resource "aws_iam_role" "lambda_roles" {
  for_each = toset(["${local.alert_lambda_scheduler_name}", "${local.alerting_lambda_name}"])

  name        = each.key
  path        = "/"
  description = "Role for ${each.key} Lambda"

  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Action = "sts:AssumeRole",
          Effect = "Allow",
          Principal = {
            Service = "lambda.amazonaws.com"
          }
        }
      ]
    }
  )

  force_detach_policies = true
}

resource "aws_iam_role" "scheduler_assume_role" {
  name = "${local.name_prefix}-scheduler-assumee"
  path = "/"
  description = join("", [
    "Role for EventBridge Scheduler to assume allowing permissions to invoke the ",
    "${local.alerting_lambda_name} Lambda"
  ])

  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Action = "sts:AssumeRole",
          Effect = "Allow",
          Principal = {
            Service = "scheduler.amazonaws.com"
          }
        }
      ]
    }
  )

  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "logs_to_lambda_roles" {
  for_each = toset(["${local.alert_lambda_scheduler_name}", "${local.alerting_lambda_name}"])

  role       = aws_iam_role.lambda_roles[each.key].name
  policy_arn = aws_iam_policy.logs[each.key].arn
}

resource "aws_iam_role_policy_attachment" "read_log_to_alerter_role" {
  role       = aws_iam_role.lambda_roles[local.alerting_lambda_name].name
  policy_arn = aws_iam_policy.read_log.arn
}

resource "aws_iam_role_policy_attachment" "scheduler_to_alert_scheduler_role" {
  role       = aws_iam_role.lambda_roles[local.alert_lambda_scheduler_name].name
  policy_arn = aws_iam_policy.scheduler.arn
}

resource "aws_iam_role_policy_attachment" "invoke_alerter_to_scheduler_assume_role" {
  role       = aws_iam_role.scheduler_assume_role.name
  policy_arn = aws_iam_policy.invoke_alerter.arn
}
