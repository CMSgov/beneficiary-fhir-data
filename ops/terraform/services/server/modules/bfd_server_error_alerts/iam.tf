resource "aws_iam_policy" "logs" {
  name = "${local.alerter_lambda_name}-logs"
  path = local.cloudtamer_iam_path
  description = join("", [
    "Permissions for the ${local.alerter_lambda_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream"
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
            "arn:aws:logs:${local.region}:${local.account_id}:log-group:/aws/lambda/${local.alerter_lambda_name}:*"
          ]
        }
      ]
    }
  )
}

resource "aws_iam_policy" "read_log" {
  name = "${local.alerter_lambda_name}-read-logs"
  path = local.cloudtamer_iam_path
  description = join("", [
    "Permissions for ${local.alerter_lambda_name} to start and retrieve the results of a Log ",
    "Insights query against the ${local.access_json_log_group_name} CloudWatch Log Group"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = ["logs:StartQuery"]
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
  path = local.cloudtamer_iam_path
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.alerter_lambda_name} Lambda"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "lambda:InvokeFunction"
          Resource = aws_lambda_function.alerter_lambda.arn
        }
      ]
    }
  )
}

resource "aws_iam_role" "alerter_lambda_role" {
  name        = local.alerter_lambda_name
  path        = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  description = "Role for ${local.alerter_lambda_name} Lambda"

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
  name = "${local.name_prefix}-scheduler-assume"
  path = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  description = join("", [
    "Role for EventBridge Scheduler to assume allowing permissions to invoke the ",
    "${local.alerter_lambda_name} Lambda"
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

resource "aws_iam_role_policy_attachment" "logs_to_alerter_lambda_role" {
  role       = aws_iam_role.alerter_lambda_role.name
  policy_arn = aws_iam_policy.logs.arn
}

resource "aws_iam_role_policy_attachment" "read_log_to_alerter_lambda_role" {
  role       = aws_iam_role.alerter_lambda_role.name
  policy_arn = aws_iam_policy.read_log.arn
}

resource "aws_iam_role_policy_attachment" "invoke_alerter_to_scheduler_assume_role" {
  role       = aws_iam_role.scheduler_assume_role.name
  policy_arn = aws_iam_policy.invoke_alerter.arn
}
