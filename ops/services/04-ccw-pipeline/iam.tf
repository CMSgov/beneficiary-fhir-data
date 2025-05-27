data "aws_iam_policy_document" "service_assume_role" {
  for_each = toset(["ecs-tasks", "lambda", "scheduler"])
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["${each.value}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "ccw_s3" {
  statement {
    sid = "AllowCCWBucketAccess"
    actions = [
      "s3:GetObject",
      "s3:ListBucket",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:GetBucketLocation",
    ]
    resources = [
      module.bucket_ccw.bucket.arn,
      "${module.bucket_ccw.bucket.arn}/*",
    ]
  }
}

resource "aws_iam_policy" "ccw_s3" {
  name        = "${local.name_prefix}-s3-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to access CCW S3 bucket"
  policy      = data.aws_iam_policy_document.ccw_s3.json
}

data "aws_iam_policy_document" "ccw_kms" {
  statement {
    sid = "AllowEnvCMKAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:ReEncrypt",
      "kms:DescribeKey",
      "kms:Encrypt"
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "ccw_kms" {
  name        = "${local.name_prefix}-kms-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to use the ${local.env} CMK"
  policy      = data.aws_iam_policy_document.ccw_kms.json
}

data "aws_iam_policy_document" "ccw_ssm_params" {
  statement {
    sid = "AllowGetPipelineAndCommonParameters"
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      for hierarchy in local.ccw_ssm_hierarchies
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(hierarchy, "/")}/*"
    ]
  }

  statement {
    sid       = "AllowDecryptParametersWithConfigCMK"
    actions   = ["kms:Decrypt"]
    resources = local.env_config_key_arns
  }
}

resource "aws_iam_policy" "ccw_ssm_params" {
  name        = "${local.name_prefix}-ssm-params-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to get required SSM parameeters"
  policy      = data.aws_iam_policy_document.ccw_ssm_params.json
}

data "aws_iam_policy_document" "ccw_metrics" {
  statement {
    sid       = "AllowCloudWatchPutMetricsInPipelineNamespace"
    actions   = ["cloudwatch:PutMetricData"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "cloudwatch:namespace"
      values   = [local.ssm_config["/bfd/${local.service}/micrometer_cw/namespace"]]
    }
  }
}

resource "aws_iam_policy" "ccw_metrics" {
  name        = "${local.name_prefix}-metrics-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to publish metrics to CloudWatch"
  policy      = data.aws_iam_policy_document.ccw_metrics.json
}

resource "aws_iam_role" "ccw_task" {
  name                  = "${local.name_prefix}-task-role"
  path                  = local.iam_path
  description           = "Role for the ${local.env} ${local.service} ECS task containers"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "ccw_task" {
  for_each = {
    s3         = aws_iam_policy.ccw_s3.arn
    kms        = aws_iam_policy.ccw_kms.arn
    ssm_params = aws_iam_policy.ccw_ssm_params.arn
    metrics    = aws_iam_policy.ccw_metrics.arn
  }

  role       = aws_iam_role.ccw_task.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "ccw_execution_ecr" {
  statement {
    sid = "AllowGetAuthTokenECR"
    actions = [
      "ecr:GetAuthorizationToken",
    ]
    # This particular action has no conditions or resource types that can constrain access.
    # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_amazonelasticcontainerregistry.html
    resources = ["*"]
  }

  statement {
    sid = "AllowPullFromECRRepos"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
    ]
    resources = [data.aws_ecr_repository.pipeline.arn]
  }
}

resource "aws_iam_policy" "ccw_execution_ecr" {
  name        = "${local.name_prefix}-execution-ecr-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to pull images from the ${data.aws_ecr_repository.pipeline.name} ECR repository"
  policy      = data.aws_iam_policy_document.ccw_execution_ecr.json
}

data "aws_iam_policy_document" "ccw_execution_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.ccw_messages.arn}:*"]
  }
}

resource "aws_iam_policy" "ccw_execution_logs" {
  name        = "${local.name_prefix}-execution-logs-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to write to its corresponding CloudWatch Log Group and Log Streams"
  policy      = data.aws_iam_policy_document.ccw_execution_logs.json
}

resource "aws_iam_role" "ccw_execution" {
  name                  = "${local.name_prefix}-execution-role"
  path                  = local.iam_path
  description           = "${local.env} ${local.service} ECS task execution role"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "ccw_execution" {
  for_each = {
    ecr  = aws_iam_policy.ccw_execution_ecr.arn
    logs = aws_iam_policy.ccw_execution_logs.arn
  }

  role       = aws_iam_role.ccw_execution.name
  policy_arn = each.value
}
