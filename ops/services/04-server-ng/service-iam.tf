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

  statement {
    sid       = "AllowDescribeRDSCluster"
    actions   = ["rds:DescribeDBClusters"]
    resources = [data.aws_rds_cluster.main.arn]
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
    resources = [
      for hierarchy in local.server_ssm_hierarchies
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(hierarchy, "/")}*"
    ]
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
    kms        = aws_iam_policy.kms.arn
    rds        = aws_iam_policy.rds.arn
    logs       = aws_iam_policy.logs.arn
    ssm_params = aws_iam_policy.ssm_params.arn
  }

  role       = aws_iam_role.service_role.name
  policy_arn = each.value
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
      data.aws_ecr_repository.log_router.arn,
      data.aws_ecr_repository.server.arn,
      "arn:aws:ecr:us-east-1:593207742271:repository/aws-guardduty-agent-fargate",
      "arn:aws:ecr:us-west-2:733349766148:repository/aws-guardduty-agent-fargate"
    ]
  }
}

resource "aws_iam_policy" "execution_ecr" {
  name        = "${local.name_prefix}-execution-ecr-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to pull images from ECR Repositories specified in the Task Definition"
  policy      = data.aws_iam_policy_document.execution_ecr.json
}

data "aws_iam_policy_document" "execution_logs" {
  statement {
    sid     = "AllowLogStreamControl"
    actions = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [
      "${aws_cloudwatch_log_group.log_router_messages.arn}:*",
      "${aws_cloudwatch_log_group.server_messages.arn}:*",
      "${aws_cloudwatch_log_group.server_access.arn}:*"
    ]
  }
}

resource "aws_iam_policy" "execution_logs" {
  name        = "${local.name_prefix}-execution-logs-policy"
  path        = local.iam_path
  description = "Grants permissions for the ${local.service} Execution Role to write to corresponding CloudWatch Log Groups and Log Streams"
  policy      = data.aws_iam_policy_document.execution_logs.json
}

resource "aws_iam_role" "execution_role" {
  name                  = "${local.name_prefix}-execution-role"
  path                  = local.iam_path
  description           = "${local.env} ${local.service} ECS task execution role"
  assume_role_policy    = data.aws_iam_policy_document.ecs_task_execution_role_assume.json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "execution" {
  for_each = {
    ecr  = aws_iam_policy.execution_ecr.arn
    logs = aws_iam_policy.execution_logs.arn
  }

  role       = aws_iam_role.execution_role.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "deploy_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "deploy_ecs" {
  statement {
    sid       = "AllowECSServiceModification"
    actions   = ["ecs:DescribeServices", "ecs:UpdateServicePrimaryTaskSet"]
    resources = ["arn:aws:ecs::${local.account_id}:service/${local.service}"]
  }

  statement {
    sid       = "AllowECSServiceTaskSetModification"
    actions   = ["ecs:CreateTaskSet", "ecs:DeleteTaskSet", ]
    resources = ["arn:aws:ecs::${local.account_id}:task-set/${local.service}/*"]
  }
}

resource "aws_iam_policy" "deploy_ecs" {
  name   = "${local.name_prefix}-deploy-ecs-policy"
  path   = local.iam_path
  policy = data.aws_iam_policy_document.deploy_ecs.json
}

data "aws_iam_policy_document" "deploy_elbv2" {
  statement {
    sid = "AllowELBDescribe"
    actions = [
      "elasticloadbalancing:DescribeListeners",
      "elasticloadbalancing:DescribeRules",
      "elasticloadbalancing:DescribeTargetGroups",
      "elasticloadbalancing:DescribeTargetHealth"
    ]
    # Unfortunately, these Describe Actions do not allow for any resource restrictions or conditions
    # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_awselasticloadbalancingv2.html#awselasticloadbalancingv2-listener-rule_net
    resources = ["*"]
  }

  statement {
    sid     = "AllowRegistrationOfTargetGroupTargets"
    actions = ["elasticloadbalancing:RegisterTargets", "elasticloadbalancing:DeregisterTargets"]
    resources = [
      for name in aws_lb_target_group.this[*].name
      : "arn:aws:elasticloadbalancing:us-east-1:${local.account_id}:targetgroup/${name}/*"
    ]
  }

  statement {
    sid       = "AllowELBListenerModification"
    actions   = ["elasticloadbalancing:ModifyListener", "elasticloadbalancing:ModifyRule"]
    resources = [for _, v in aws_lb_listener.this : v.arn]
  }

  statement {
    sid       = "AllowELBListenerRuleModification"
    actions   = ["elasticloadbalancing:ModifyRule"]
    resources = [for _, v in aws_lb_listener.this : "${replace(v.arn, "listener/", "listener-rule/")}/*"]
  }
}

resource "aws_iam_policy" "deploy_elbv2" {
  name   = "${local.name_prefix}-deploy-elbv2-policy"
  path   = local.iam_path
  policy = data.aws_iam_policy_document.deploy_elbv2.json
}

data "aws_iam_policy_document" "deploy_iam" {
  statement {
    sid       = "AllowPassRole"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.service_role.arn, aws_iam_role.execution_role.arn]
  }
}

resource "aws_iam_policy" "deploy_iam" {
  name   = "${local.name_prefix}-deploy-iam-policy"
  path   = local.iam_path
  policy = data.aws_iam_policy_document.deploy_iam.json
}

resource "aws_iam_role" "deploy" {
  name = "${local.name_prefix}-deploy"
  # path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  assume_role_policy    = data.aws_iam_policy_document.deploy_assume.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "deploy" {
  for_each = {
    ecs   = aws_iam_policy.deploy_ecs.arn
    elbv2 = aws_iam_policy.deploy_elbv2.arn
    iam   = aws_iam_policy.deploy_iam.arn
  }

  role       = aws_iam_role.deploy.name
  policy_arn = each.value
}
