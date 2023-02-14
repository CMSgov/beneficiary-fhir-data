resource "aws_iam_policy" "cloudwatch_metrics" {
  name        = "${local.lambda_full_name}-cloudwatch-metrics"
  description = "Permissions for the ${local.lambda_full_name} Lambda to put and get metric data"

  # Unfortunately, neither GetMetricData nor PutMetricData support resource-level permissions, and
  # only PutMetricData supports the cloudwatch:namespace condition. This is why they're both so
  # permissive
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "cloudwatch:GetMetricData",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "cloudwatch:PutMetricData",
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "cloudwatch:namespace": "${local.metrics_namespace}"
        }
      }
    }
  ]
}
EOF
}
resource "aws_iam_policy" "logs" {
  name = "${local.lambda_full_name}-logs"
  description = join("", [
    "Permissions for the ${local.lambda_full_name} Lambda to write to its corresponding CloudWatch ",
    "Log Group and Log Stream"
  ])

  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "logs:CreateLogGroup",
      "Resource": "arn:aws:logs:${local.region}:${var.account_id}:*"
    },
    {
      "Effect": "Allow",
      "Action": ["logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": [
        "arn:aws:logs:${local.region}:${var.account_id}:log-group:/aws/lambda/${local.lambda_full_name}:*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_role" "this" {
  name        = local.lambda_full_name
  path        = "/"
  description = "Role for ${local.lambda_full_name} Lambda"

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
    aws_iam_policy.cloudwatch_metrics.arn,
    aws_iam_policy.logs.arn
  ]
}
