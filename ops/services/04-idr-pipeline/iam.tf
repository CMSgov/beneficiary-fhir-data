data "aws_iam_policy_document" "service_assume_role" {
  for_each = toset(["ecs-tasks"])
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["${each.value}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "idr_ecs_exec" {
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

resource "aws_iam_policy" "idr_ecs_exec" {
  name        = "${local.name_prefix}-ecs-exec-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to use ECS Exec"
  policy      = data.aws_iam_policy_document.idr_ecs_exec.json
}

data "aws_iam_policy_document" "idr_kms" {
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

resource "aws_iam_policy" "idr_kms" {
  name        = "${local.name_prefix}-kms-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to use the ${local.env} CMK"
  policy      = data.aws_iam_policy_document.idr_kms.json
}

data "aws_iam_policy_document" "idr_ssm_params" {
  statement {
    sid = "AllowGetPipelineAndCommonParameters"
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      for hierarchy in local.idr_ssm_hierarchies
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(hierarchy, "/")}/*"
    ]
  }

  statement {
    sid       = "AllowDecryptParametersWithEnvCMK"
    actions   = ["kms:Decrypt"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "idr_ssm_params" {
  name        = "${local.name_prefix}-ssm-params-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to get required SSM parameeters"
  policy      = data.aws_iam_policy_document.idr_ssm_params.json
}

data "aws_iam_policy_document" "idr_rds" {
  statement {
    sid       = "AllowDescribeRDSCluster"
    actions   = ["rds:DescribeDBClusters"]
    resources = [data.aws_rds_cluster.main.arn]
  }
}

resource "aws_iam_policy" "idr_rds" {
  name        = "${local.name_prefix}-rds-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to describe RDS Cluster"
  policy      = data.aws_iam_policy_document.idr_rds.json
}

resource "aws_iam_role" "idr_task" {
  name                  = "${local.name_prefix}-task-role"
  path                  = local.iam_path
  description           = "Role for the ${local.env} ${local.service} ECS task containers"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "idr_task" {
  for_each = {
    kms        = aws_iam_policy.idr_kms.arn
    rds        = aws_iam_policy.idr_rds.arn
    ssm_params = aws_iam_policy.idr_ssm_params.arn
    ecs_exec   = aws_iam_policy.idr_ecs_exec.arn
  }

  role       = aws_iam_role.idr_task.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "idr_execution_ecr" {
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

resource "aws_iam_policy" "idr_execution_ecr" {
  name        = "${local.name_prefix}-execution-ecr-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to pull images from the ${data.aws_ecr_repository.pipeline.name} ECR repository"
  policy      = data.aws_iam_policy_document.idr_execution_ecr.json
}

data "aws_iam_policy_document" "idr_execution_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.idr_messages.arn}:*"]
  }
}

resource "aws_iam_policy" "idr_execution_logs" {
  name        = "${local.name_prefix}-execution-logs-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to write to its corresponding CloudWatch Log Group and Log Streams"
  policy      = data.aws_iam_policy_document.idr_execution_logs.json
}

resource "aws_iam_role" "idr_execution" {
  name                  = "${local.name_prefix}-execution-role"
  path                  = local.iam_path
  description           = "${local.env} ${local.service} ECS task execution role"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "idr_execution" {
  for_each = {
    ecr  = aws_iam_policy.idr_execution_ecr.arn
    logs = aws_iam_policy.idr_execution_logs.arn
  }

  role       = aws_iam_role.idr_execution.name
  policy_arn = each.value
}
