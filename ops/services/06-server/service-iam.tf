data "aws_iam_policy_document" "certstores_s3" {
  statement {
    sid = "AllowGetCertstores"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    resources = [
      aws_s3_bucket.certstores.arn,
      "${aws_s3_bucket.certstores.arn}/${local.keystore_s3_key}",
      "${aws_s3_bucket.certstores.arn}/${local.truststore_s3_key}"
    ]
  }
}

resource "aws_iam_policy" "certstores_s3" {
  name        = "${local.name_prefix}-certstores-s3-policy"
  path        = local.cloudtamer_iam_path
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
    resources = [data.aws_kms_alias.env_cmk.target_key_arn]
  }
}

resource "aws_iam_policy" "kms" {
  name        = "${local.name_prefix}-kms-policy"
  path        = local.cloudtamer_iam_path
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
  path        = local.cloudtamer_iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS service containers to describe RDS DB Instances"
  policy      = data.aws_iam_policy_document.rds.json
}

data "aws_iam_policy_document" "ssm_params" {
  statement {
    sid = "AllowGetServerAndCommonParameters"
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      for hierarchy in local.server_ssm_hierarchies
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(hierarchy, "/")}/*"
    ]
  }

  statement {
    sid       = "AllowDecryptParametersWithConfigCMK"
    actions   = ["kms:Decrypt"]
    resources = [data.aws_kms_alias.env_config_cmk.target_key_arn]
  }
}

resource "aws_iam_policy" "ssm_params" {
  name        = "${local.name_prefix}-ssm-params-policy"
  path        = local.cloudtamer_iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS service containers to get required SSM parameeters"
  policy      = data.aws_iam_policy_document.ssm_params.json
}

data "aws_iam_policy_document" "ecs_exec" {
  statement {
    sid = "AllowECSExec"
    actions = [
      "ssmmessages:CreateDataChannel",
      "ssmmessages:OpenDataChannel",
      "ssmmessages:OpenControlChannel",
      "ssmmessages:CreateControlChannel"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "ecs_exec" {
  name        = "${local.name_prefix}-ecs-exec-policy"
  path        = local.cloudtamer_iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS service containers to use ECS Exec"
  policy      = data.aws_iam_policy_document.ecs_exec.json
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
  path                  = local.cloudtamer_iam_path
  description           = "Role for the ${local.env} ${local.service} ECS service containers"
  assume_role_policy    = data.aws_iam_policy_document.ecs_service_role_assume.json
  permissions_boundary  = data.aws_iam_policy.permissions_boundary.arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "service_role" {
  for_each = {
    certstores_s3 = aws_iam_policy.certstores_s3.arn
    kms           = aws_iam_policy.kms.arn
    rds           = aws_iam_policy.rds.arn
    ssm_params    = aws_iam_policy.ssm_params.arn
    ecs_exec      = aws_iam_policy.ecs_exec.arn
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
  path                  = local.cloudtamer_iam_path
  description           = "${local.env} ${local.service} ECS task execution role"
  assume_role_policy    = data.aws_iam_policy_document.ecs_task_execution_role_assume.json
  permissions_boundary  = data.aws_iam_policy.permissions_boundary.arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "execution_role" {
  role       = aws_iam_role.execution_role.name
  policy_arn = data.aws_iam_policy.ecs_execution_role.arn
}
