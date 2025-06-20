locals {
  full_name = "bfd-insights-${var.name}"
}

resource "aws_iam_group" "main" {
  name = local.full_name
}

resource "aws_iam_group_policy_attachment" "main_attach" {
  count       = length(var.policy_arns)
  group       = aws_iam_group.main.name
  policy_arn  = var.policy_arns[count.index]
}

