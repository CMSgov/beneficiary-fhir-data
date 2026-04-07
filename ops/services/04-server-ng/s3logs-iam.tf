data "aws_iam_policy_document" "s3logs_firehose_logs" {
  statement {
    sid       = "AllowPutLogEventsAndCreateStream"
    actions   = ["logs:PutLogEvents", "logs:CreateLogStream"]
    resources = ["${aws_cloudwatch_log_group.s3logs.arn}:log-stream:*"]
  }
}

resource "aws_iam_policy" "s3logs_firehose_logs" {
  name = "${local.s3logs_firehose_name}-firehose-logs-policy"
  path = local.iam_path
  description = join("", [
    "Grants permission for the ${local.s3logs_firehose_name} Firehose Data Stream to write to its ",
    "corresponding CloudWatch Logs Log Streams"
  ])
  policy = data.aws_iam_policy_document.s3logs_firehose_logs.json
}

data "aws_iam_policy_document" "s3logs_firehose_s3" {
  statement {
    actions = [
      "s3:AbortMultipartUpload",
      "s3:GetBucketLocation",
      "s3:GetObject",
      "s3:ListBucket",
      "s3:ListBucketMultipartUploads",
      "s3:PutObject"
    ]
    resources = [module.bucket_s3logs.bucket.arn, "${module.bucket_s3logs.bucket.arn}/*"]
  }
}

resource "aws_iam_policy" "s3logs_firehose_s3" {
  name = "${local.s3logs_firehose_name}-firehose-s3-policy"
  path = local.iam_path
  description = join("", [
    "Grants permission for the ${local.s3logs_firehose_name} Firehose Data Stream to use the ",
    "${module.bucket_s3logs.bucket.bucket} Bucket"
  ])
  policy = data.aws_iam_policy_document.s3logs_firehose_s3.json
}

data "aws_iam_policy_document" "s3logs_firehose_kms" {
  statement {
    actions = [
      "kms:GenerateDataKey",
      "kms:Decrypt",
      "kms:Encrypt"
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "s3logs_firehose_kms" {
  name = "${local.s3logs_firehose_name}-firehose-kms-policy"
  path = local.iam_path
  description = join("", [
    "Grants permission for the ${local.s3logs_firehose_name} Firehose Data Stream to use the ",
    "${local.env_key_alias} KMS key"
  ])
  policy = data.aws_iam_policy_document.s3logs_firehose_kms.json
}

resource "aws_iam_role" "s3logs_firehose" {
  name                  = "${local.s3logs_firehose_name}-firehose-role"
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role for ${local.s3logs_firehose_name}"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["firehose"].json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "s3logs_firehose" {
  for_each = {
    logs = aws_iam_policy.s3logs_firehose_logs.arn
    s3   = aws_iam_policy.s3logs_firehose_s3.arn
    kms  = aws_iam_policy.s3logs_firehose_kms.arn
  }

  role       = aws_iam_role.s3logs_firehose.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "s3logs_cw_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["logs.amazonaws.com"]
    }

    condition {
      test     = "StringLike"
      variable = "aws:SourceArn"
      values   = ["arn:aws:logs:${local.region}:${local.account_id}:*"]
    }
  }
}

data "aws_iam_policy_document" "s3logs_cw_firehose" {
  statement {
    actions   = ["firehose:PutRecord"]
    resources = [aws_kinesis_firehose_delivery_stream.s3logs.arn]
  }
}

resource "aws_iam_policy" "s3logs_cw_firehose" {
  name = "${local.s3logs_firehose_name}-cw-firehose-policy"
  path = local.iam_path
  description = join("", [
    "Grants permission for CloudWatch Logs to put records to the ",
    "${aws_kinesis_firehose_delivery_stream.s3logs.name} Firehose Data Stream"
  ])
  policy = data.aws_iam_policy_document.s3logs_cw_firehose.json
}

resource "aws_iam_role" "s3logs_cw" {
  name                  = "${local.s3logs_firehose_name}-cw-role"
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Allows CloudWatch Logs to send records to Firehose"
  assume_role_policy    = data.aws_iam_policy_document.s3logs_cw_assume.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "s3logs_cw" {
  for_each = {
    firehose = aws_iam_policy.s3logs_cw_firehose.arn
  }

  role       = aws_iam_role.s3logs_cw.name
  policy_arn = each.value
}
