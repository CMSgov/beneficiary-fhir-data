data "aws_iam_policy_document" "logs_policy_doc" {
  statement {
    sid       = "AllowLogGroupCreate"
    actions   = ["logs:CreateLogGroup"]
    resources = ["arn:aws:logs:${local.region}:${local.account_id}:*"]
  }

  statement {
    sid     = "AllowLogStreamControl"
    actions = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [
      "arn:aws:logs:${local.region}:${local.account_id}:log-group:/aws/lambda/${local.lambda_full_name}:*"
    ]
  }
}

resource "aws_iam_policy" "logs" {
  name = "${local.lambda_full_name}-logs"
  description = join("", [
    "Permissions for the ${local.lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = data.aws_iam_policy_document.logs_policy_doc.json
}

data "aws_iam_policy_document" "rds_policy_doc" {
  statement {
    sid       = "AllowModifyDBInstance"
    actions   = ["rds:ModifyDBInstance"]
    resources = ["arn:aws:rds:${local.region}:${local.account_id}:db:*"]
    condition {
      test     = "StringEquals"
      variable = "rds:db-tag/application-autoscaling:resourceId"
      values   = ["cluster:${var.db_cluster_identifier}"]
    }
  }

  statement {
    sid       = "AllowPassingMonitoringRole"
    actions   = ["iam:PassRole"]
    resources = [var.rds_monitoring_role_arn]
  }
}

resource "aws_iam_policy" "rds" {
  name        = "${local.lambda_full_name}-rds"
  description = "Permissions to modify Application AutoScaling DB Instances in ${var.db_cluster_identifier}"
  policy      = data.aws_iam_policy_document.rds_policy_doc.json
}

data "aws_iam_policy_document" "kms_policy_doc" {
  statement {
    sid = "AllowEncryptionAndDecryptionOfMasterKeys"
    actions = [
      "kms:Encrypt",
      "kms:Decrypt",
      "kms:ReEncrypt*",
      "kms:GenerateDataKey*",
      "kms:DescribeKey",
    ]
    resources = [local.kms_key_id]
  }
}

resource "aws_iam_policy" "kms" {
  name = "${local.lambda_full_name}-kms"
  description = join("", [
    "Permissions to decrypt config KMS keys and encrypt and decrypt master KMS keys for ",
    "${local.env}"
  ])

  policy = data.aws_iam_policy_document.kms_policy_doc.json
}

data "aws_iam_policy_document" "role_assume_policy_doc" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "this" {
  name                  = local.lambda_full_name
  path                  = "/"
  description           = "Role for ${local.lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.role_assume_policy_doc.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "this" {
  for_each = {
    logs = aws_iam_policy.logs.arn,
    rds  = aws_iam_policy.rds.arn
    kms  = aws_iam_policy.kms.arn,
  }

  role       = aws_iam_role.this.name
  policy_arn = each.value
}
