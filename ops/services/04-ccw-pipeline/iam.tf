locals {
  ccw_rif_role_name = !var.greenfield ? "bfd-${local.env}-ccw-rif" : "${local.name_prefix}-ccw-rif-upload-role"
}

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
    sid       = "AllowDecryptParametersWithEnvCMK"
    actions   = ["kms:Decrypt"]
    resources = [local.env_key_arn]
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
    resources = [
      data.aws_ecr_repository.pipeline.arn,
      "arn:aws:ecr:us-east-1:593207742271:repository/aws-guardduty-agent-fargate",
      "arn:aws:ecr:us-west-2:733349766148:repository/aws-guardduty-agent-fargate"
    ]
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

data "aws_iam_policy_document" "ccw_rif_trust" {
  count = local.is_prod ? 1 : 0

  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type = "AWS"
      identifiers = [
        for arn in split(" ", local.ssm_config["/bfd/${local.service}/ccw/ccw_rif_role_principal_arns"]) : trimspace(arn)
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "sts:ExternalId"
      values   = [local.ssm_config["/bfd/${local.service}/ccw/ccw_rif_role_external_id"]]
    }
  }
}

data "aws_iam_policy_document" "ccw_rif_kms" {
  count = local.is_prod ? 1 : 0

  statement {
    actions   = ["kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "ccw_rif_kms" {
  count = local.is_prod ? 1 : 0

  name        = "${local.ccw_rif_role_name}-kms-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.ccw_rif_role_name} IAM Role to use the ${local.env} KMS key"
  policy      = one(data.aws_iam_policy_document.ccw_rif_kms[*].json)
}

data "aws_iam_policy_document" "ccw_rif_s3" {
  count = local.is_prod ? 1 : 0

  statement {
    sid       = "AllowListingOfCCWBucket"
    actions   = ["s3:ListBucket"]
    resources = [module.bucket_ccw.bucket.arn]
  }

  statement {
    sid       = "AllowPuttingAndGettingObjectsInCCWBucket"
    actions   = ["s3:GetObject", "s3:PutObject"]
    resources = ["${module.bucket_ccw.bucket.arn}/*"]
  }
}

resource "aws_iam_policy" "ccw_rif_s3" {
  count = local.is_prod ? 1 : 0

  name        = "${local.ccw_rif_role_name}-s3-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.ccw_rif_role_name} IAM Role to interact with the ${module.bucket_ccw.bucket.bucket} Bucket"
  policy      = one(data.aws_iam_policy_document.ccw_rif_s3[*].json)
}

resource "aws_iam_role" "ccw_rif" {
  count = local.is_prod ? 1 : 0

  name                  = local.ccw_rif_role_name
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  description           = "Role assumed by CCW to read and write to the ${local.env} production and verification ETL buckets."
  max_session_duration  = 43200 # max session duration is 12 hours (43200 seconds)- going big for long data-loads
  assume_role_policy    = one(data.aws_iam_policy_document.ccw_rif_trust[*].json)
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "ccw_rif" {
  for_each = local.is_prod ? {
    kms = one(aws_iam_policy.ccw_rif_kms[*].arn)
    s3  = one(aws_iam_policy.ccw_rif_s3[*].arn)
  } : {}

  role       = one(aws_iam_role.ccw_rif[*].name)
  policy_arn = each.value
}
