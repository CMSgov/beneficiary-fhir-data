data "aws_iam_policy_document" "service_assume_role" {
  for_each = toset(["ecs-tasks", "scheduler"])
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["${each.value}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "kms" {
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

resource "aws_iam_policy" "kms" {
  name        = "${local.name_prefix}-kms-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to use the ${local.env} CMK"
  policy      = data.aws_iam_policy_document.kms.json
}

data "aws_iam_policy_document" "ssm_params" {
  statement {
    sid = "AllowGetPipelineAndCommonParameters"
    actions = [
      "ssm:GetParametersByPath",
      "ssm:GetParameters",
      "ssm:GetParameter"
    ]
    resources = [
      for hierarchy in local.npi_ssm_hierarchies
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(hierarchy, "/")}/*"
    ]
  }
}

resource "aws_iam_policy" "ssm_params" {
  name        = "${local.name_prefix}-ssm-params-policy"
  path        = local.iam_path
  description = "Permissions for the ${local.env} ${local.service} ECS task containers to get required SSM parameeters"
  policy      = data.aws_iam_policy_document.ssm_params.json
}

resource "aws_iam_role" "service" {
  name                  = "${local.name_prefix}-task-role"
  path                  = local.iam_path
  description           = "Role for the ${local.env} ${local.service} ECS task containers"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "service" {
  for_each = {
    kms        = aws_iam_policy.kms.arn
    ssm_params = aws_iam_policy.ssm_params.arn
  }

  role       = aws_iam_role.service.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "execution_ecr" {
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

resource "aws_iam_policy" "execution_ecr" {
  name        = "${local.name_prefix}-execution-ecr-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to pull images from the ${data.aws_ecr_repository.pipeline.name} ECR repository"
  policy      = data.aws_iam_policy_document.execution_ecr.json
}

data "aws_iam_policy_document" "execution_logs" {
  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.messages.arn}:*"]
  }
}

resource "aws_iam_policy" "execution_logs" {
  name        = "${local.name_prefix}-execution-logs-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to write to its corresponding CloudWatch Log Group and Log Streams"
  policy      = data.aws_iam_policy_document.execution_logs.json
}

resource "aws_iam_role" "execution" {
  name                  = "${local.name_prefix}-execution-role"
  path                  = local.iam_path
  description           = "${local.env} ${local.service} ECS task execution role"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs-tasks"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "execution" {
  for_each = {
    ecr  = aws_iam_policy.execution_ecr.arn
    logs = aws_iam_policy.execution_logs.arn
  }

  role       = aws_iam_role.execution.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "schedule_ecs" {
  statement {
    sid       = "AllowRunEcsTask"
    actions   = ["ecs:RunTask"]
    resources = [aws_ecs_task_definition.this.arn]

    condition {
      test     = "ArnEquals"
      variable = "ecs:cluster"
      values   = [data.aws_ecs_cluster.main.arn]
    }
  }
}

resource "aws_iam_policy" "schedule_ecs" {
  name        = "${local.name_prefix}-schedule-ecs-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Schedule to run ${local.service} ECS Tasks"
  policy      = data.aws_iam_policy_document.schedule_ecs.json
}

data "aws_iam_policy_document" "schedule_iam" {
  statement {
    sid       = "AllowPassRole"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.schedule.arn, aws_iam_role.execution.arn]
  }
}

resource "aws_iam_policy" "schedule_iam" {
  name   = "${local.name_prefix}-schedule-iam-policy"
  path   = local.iam_path
  policy = data.aws_iam_policy_document.schedule_iam.json
}

resource "aws_iam_role" "schedule" {
  name                  = "${local.name_prefix}-schedule-role"
  path                  = local.iam_path
  description           = "${local.env} ${local.service} Scheduler Role"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["scheduler"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "schedule" {
  for_each = {
    ecs = aws_iam_policy.schedule_ecs.arn
    iam = aws_iam_policy.schedule_iam.arn
  }

  role       = aws_iam_role.schedule.name
  policy_arn = each.value
}
