data "aws_iam_policy_document" "logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.this.arn}:*"]
  }
}

resource "aws_iam_policy" "logs" {
  name = "${var.lambda_name}-logs"
  path = var.iam_path
  description = join("", [
    "Grants permissions for the ${var.lambda_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])
  policy = data.aws_iam_policy_document.logs.json
}

data "aws_iam_policy_document" "glue" {
  statement {
    sid       = "AllowStartGlueCrawler"
    actions   = ["glue:StartCrawler"]
    resources = [var.crawler_arn]
  }

  statement {
    sid     = "AllowGetGluePartitions"
    actions = ["glue:GetPartition"]
    resources = [
      "arn:aws:glue:${local.region}:${local.account_id}:catalog",
      "arn:aws:glue:${local.region}:${local.account_id}:database/${var.database_name}",
      "arn:aws:glue:${local.region}:${local.account_id}:table/${var.database_name}/${var.table_name}"
    ]
  }
}

resource "aws_iam_policy" "glue" {
  name = "${var.lambda_name}-glue"
  path = var.iam_path
  description = join("", [
    "Grants permissions for the ${var.lambda_name} Lambda to start the ",
    "${var.crawler_name} Glue Crawler"
  ])
  policy = data.aws_iam_policy_document.glue.json
}

data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name                  = "${var.lambda_name}-role"
  path                  = var.iam_path
  description           = "Role for the ${var.lambda_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.lambda_assume.json
  permissions_boundary  = var.iam_permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "this" {
  for_each = {
    logs = aws_iam_policy.logs.arn
    glue = aws_iam_policy.glue.arn
  }

  role       = aws_iam_role.this.name
  policy_arn = each.value
}
