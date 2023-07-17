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
  })
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

resource "aws_iam_role_policy_attachment" "logs_to_lambda_roles" {
  for_each = toset(["${local.alert_lambda_scheduler_name}", "${local.alerting_lambda_name}"])

  role       = aws_iam_role.lambda_roles[each.key].name
  policy_arn = aws_iam_policy.logs[each.key].arn
}
