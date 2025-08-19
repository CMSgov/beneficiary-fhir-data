data "aws_iam_policy_document" "logs" {
  statement {
    sid = "AllowLogControl"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:PutMetricFilter",
      "logs:PutRetentionPolicy"
    ]
    resources = [
      "${aws_cloudwatch_log_group.success.arn}:*",
      "${aws_cloudwatch_log_group.failure.arn}:*"
    ]
  }
}

resource "aws_iam_policy" "logs" {
  name = "${var.topic_name}-logs-policy"
  path = var.iam_path
  description = join("", [
    "Permissions for the ${var.topic_name} SNS Topic to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])
  policy = data.aws_iam_policy_document.logs.json
}

data "aws_iam_policy_document" "sns_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name                  = "${var.topic_name}-role"
  path                  = var.iam_path
  description           = "Role for the ${var.topic_name} SNS Topic"
  assume_role_policy    = data.aws_iam_policy_document.sns_assume_role.json
  permissions_boundary  = var.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "this" {
  for_each = {
    logs = aws_iam_policy.logs.arn
  }

  role       = aws_iam_role.this.name
  policy_arn = each.value
}
