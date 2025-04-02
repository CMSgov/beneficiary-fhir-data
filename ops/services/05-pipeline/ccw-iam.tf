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
  name        = "${local.ccw_name_prefix}-s3-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.ccw_task_name} ECS task containers to access CCW S3 bucket"
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
  name        = "${local.ccw_name_prefix}-kms-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.ccw_task_name} ECS task containers to use the ${local.env} CMK"
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
  name        = "${local.ccw_name_prefix}-ssm-params-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.ccw_task_name} ECS task containers to get required SSM parameeters"
  policy      = data.aws_iam_policy_document.ccw_ssm_params.json
}

data "aws_iam_policy_document" "ccw_ecs_exec" {
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

resource "aws_iam_policy" "ccw_ecs_exec" {
  name        = "${local.ccw_name_prefix}-ecs-exec-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.ccw_task_name} ECS task containers to use ECS Exec"
  policy      = data.aws_iam_policy_document.ccw_ecs_exec.json
}

resource "aws_iam_role" "ccw_task" {
  name                  = "${local.ccw_name_prefix}-task-role"
  path                  = local.iam_path
  description           = "Role for the ${local.env} ${local.ccw_task_name} ECS task containers"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "ccw_task" {
  for_each = {
    s3         = aws_iam_policy.ccw_s3.arn
    kms        = aws_iam_policy.ccw_kms.arn
    ssm_params = aws_iam_policy.ccw_ssm_params.arn
    ecs_exec   = aws_iam_policy.ccw_ecs_exec.arn
  }

  role       = aws_iam_role.ccw_task.name
  policy_arn = each.value
}

# TODO: Custom execution role
data "aws_iam_policy" "ecs_execution_role" {
  name = "AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ccw_execution" {
  name                  = "${local.ccw_name_prefix}-execution-role"
  path                  = local.iam_path
  description           = "${local.env} ${local.ccw_task_name} ECS task execution role"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "ccw_execution" {
  role       = aws_iam_role.ccw_execution.name
  policy_arn = data.aws_iam_policy.ecs_execution_role.arn
}
