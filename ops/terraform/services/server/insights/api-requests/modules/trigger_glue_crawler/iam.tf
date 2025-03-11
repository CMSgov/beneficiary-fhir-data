resource "aws_iam_policy" "glue" {
  name = "${local.lambda_full_name}-glue"
  path = var.cloudtamer_iam_path
  description = join("", [
    "Permissions for the ${local.lambda_full_name} Lambda to start the ${var.glue_crawler_name} ",
    "Glue crawler and to query specific partitions on the ${var.glue_table} Glue Table"
  ])
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "glue:StartCrawler",
      "Resource": "${var.glue_crawler_arn}"
    },
    {
      "Effect": "Allow",
      "Action": "glue:GetPartition",
      "Resource": [
        "arn:aws:glue:us-east-1:${var.account_id}:catalog",
        "arn:aws:glue:us-east-1:${var.account_id}:database/${var.glue_database}",
        "arn:aws:glue:us-east-1:${var.account_id}:table/${var.glue_database}/${var.glue_table}"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "logs" {
  name = "${local.lambda_full_name}-logs"
  path = var.cloudtamer_iam_path
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
      "Resource": "arn:aws:logs:us-east-1:${var.account_id}:*"
    },
    {
      "Effect": "Allow",
      "Action": ["logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": [
        "arn:aws:logs:us-east-1:${var.account_id}:log-group:/aws/lambda/${local.lambda_full_name}:*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_role" "this" {
  name        = local.lambda_full_name
  path        = var.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
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
    aws_iam_policy.logs.arn,
    aws_iam_policy.glue.arn
  ]
}
