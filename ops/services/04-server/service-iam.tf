data "aws_iam_policy_document" "certstores_s3" {
  statement {
    sid = "AllowGetCertstores"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    resources = [
      module.bucket_certstores.bucket.arn,
      "${module.bucket_certstores.bucket.arn}/${local.keystore_s3_key}",
      "${module.bucket_certstores.bucket.arn}/${local.truststore_s3_key}"
    ]
  }
}

resource "aws_iam_policy" "certstores_s3" {
  name        = "${local.name_prefix}-certstores-s3-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS service containers to pull certstores from S3"
  policy      = data.aws_iam_policy_document.certstores_s3.json
}

data "aws_iam_policy_document" "kms" {
  statement {
    sid = "AllowEnvCMKAccess"
    actions = [
      "kms:Decrypt",
      "kms:GenerateDataKey",
      "kms:ReEncrypt",
      "kms:DescribeKey",
      "kms:CreateGrant",
      "kms:ListGrants",
      "kms:RevokeGrant"
    ]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "kms" {
  name        = "${local.name_prefix}-kms-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS service containers to use the ${local.env} CMK"
  policy      = data.aws_iam_policy_document.kms.json
}

data "aws_iam_policy_document" "rds" {
  statement {
    sid       = "AllowDescribeRDSInstances"
    actions   = ["rds:DescribeDBInstances"]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "rds" {
  name        = "${local.name_prefix}-rds-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS service containers to describe RDS DB Instances"
  policy      = data.aws_iam_policy_document.rds.json
}

data "aws_iam_policy_document" "logs" {
  statement {
    sid       = "AllowFireLensPutLogEventsAndCreateStream"
    actions   = ["logs:PutLogEvents", "logs:CreateLogStream"]
    resources = ["${aws_cloudwatch_log_group.server_access.arn}:log-stream:*", "${aws_cloudwatch_log_group.server_messages.arn}:log-stream:*"]
  }
}

resource "aws_iam_policy" "logs" {
  name        = "${local.name_prefix}-logs-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} firelens sidecar container to submit logs from the ${local.service} container"
  policy      = data.aws_iam_policy_document.logs.json
}

data "aws_iam_policy_document" "ssm_params" {
  statement {
    sid = "AllowGetServerAndCommonParameters"
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = flatten([
      for hierarchy in local.server_ssm_hierarchies
      : [
        "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(hierarchy, "/")}/*",
        # TODO: Remove this, and flatten, when "/ng/" prefix is removed from new config
        "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(replace(hierarchy, "/ng/", ""), "/")}/*",
      ]
    ])
  }

  statement {
    sid       = "AllowDecryptParametersWithConfigCMK"
    actions   = ["kms:Decrypt"]
    resources = local.env_config_key_arns
  }
}

resource "aws_iam_policy" "ssm_params" {
  name        = "${local.name_prefix}-ssm-params-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS service containers to get required SSM parameeters"
  policy      = data.aws_iam_policy_document.ssm_params.json
}

data "aws_iam_policy_document" "ecs_service_role_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "service_role" {
  name                  = "${local.name_prefix}-service-role"
  path                  = local.iam_path
  description           = "Role for the ${local.env} ${local.service} ECS service containers"
  assume_role_policy    = data.aws_iam_policy_document.ecs_service_role_assume.json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "service_role" {
  for_each = {
    certstores_s3 = aws_iam_policy.certstores_s3.arn
    kms           = aws_iam_policy.kms.arn
    rds           = aws_iam_policy.rds.arn
    logs          = aws_iam_policy.logs.arn
    ssm_params    = aws_iam_policy.ssm_params.arn
  }

  role       = aws_iam_role.service_role.name
  policy_arn = each.value
}

data "aws_iam_policy" "ecs_execution_role" {
  name = "AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "ecs_task_execution_role_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution_role" {
  name                  = "${local.name_prefix}-execution-role"
  path                  = local.iam_path
  description           = "${local.env} ${local.service} ECS task execution role"
  assume_role_policy    = data.aws_iam_policy_document.ecs_task_execution_role_assume.json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "execution_role" {
  role       = aws_iam_role.execution_role.name
  policy_arn = data.aws_iam_policy.ecs_execution_role.arn
}
