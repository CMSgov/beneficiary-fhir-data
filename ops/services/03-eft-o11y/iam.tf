resource "aws_iam_policy" "slack_notifier_logs" {
  name = "${local.slack_notifier_lambda_name}-logs"
  path = local.cloudtamer_iam_path
  description = join("", [
    "Permissions for the ${local.slack_notifier_lambda_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
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
            "arn:aws:logs:${local.region}:${local.account_id}:log-group:/aws/lambda/${local.slack_notifier_lambda_name}:*"
          ]
        }
      ]
    }
  )
}

resource "aws_iam_role" "slack_notifier" {
  name                 = local.slack_notifier_lambda_name
  path                 = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  description          = "Role for ${local.slack_notifier_lambda_name} Lambda"

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

resource "aws_iam_role_policy_attachment" "slack_notifier" {
  for_each = {
    for key, arn in {
      logs = one(aws_iam_policy.slack_notifier_logs[*].arn),
    } : key => arn
  }

  role       = one(aws_iam_role.slack_notifier[*].name)
  policy_arn = each.value
}
