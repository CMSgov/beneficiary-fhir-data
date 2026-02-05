data "aws_iam_policy_document" "codedeploy_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codedeploy.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "codedeploy_ecs" {
  statement {
    sid       = "AllowECSServiceModification"
    actions   = ["ecs:DescribeServices", "ecs:UpdateServicePrimaryTaskSet"]
    resources = [aws_ecs_service.server.id]
  }

  statement {
    sid       = "AllowECSServiceTaskSetModification"
    actions   = ["ecs:CreateTaskSet", "ecs:DeleteTaskSet", ]
    resources = ["${replace(aws_ecs_service.server.id, "service/", "task-set/")}/*"]
  }
}

resource "aws_iam_policy" "codedeploy_ecs" {
  name   = "${local.name_prefix}-codedeploy-ecs-policy"
  path   = local.iam_path
  policy = data.aws_iam_policy_document.codedeploy_ecs.json
}

data "aws_iam_policy_document" "codedeploy_elbv2" {
  statement {
    sid = "AllowELBDescribe"
    actions = [
      "elasticloadbalancing:DescribeListeners",
      "elasticloadbalancing:DescribeRules",
      "elasticloadbalancing:DescribeTargetGroups",
    ]
    # Unfortunately, these Describe Actions do not allow for any resource restrictions or conditions
    # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_awselasticloadbalancingv2.html#awselasticloadbalancingv2-listener-rule_net
    resources = ["*"]
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

resource "aws_iam_policy" "codedeploy_elbv2" {
  name   = "${local.name_prefix}-codedeploy-elbv2-policy"
  path   = local.iam_path
  policy = data.aws_iam_policy_document.codedeploy_elbv2.json
}

data "aws_iam_policy_document" "codedeploy_iam" {
  statement {
    sid       = "AllowPassRole"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.service_role.arn, aws_iam_role.execution_role.arn]
  }
}

resource "aws_iam_policy" "codedeploy_iam" {
  name   = "${local.name_prefix}-codedeploy-iam-policy"
  path   = local.iam_path
  policy = data.aws_iam_policy_document.codedeploy_iam.json
}

data "aws_iam_policy_document" "codedeploy_lambda" {
  count = local.regression_wrapper_enabled ? 1 : 0

  statement {
    sid       = "AllowUsageOfRegressionWrapperLambda"
    actions   = ["lambda:InvokeFunction", "lambda:GetFunction"]
    resources = aws_lambda_function.regression_wrapper[*].arn
  }
}

resource "aws_iam_policy" "codedeploy_lambda" {
  count = local.regression_wrapper_enabled ? 1 : 0

  name   = "${local.name_prefix}-codedeploy-lambda-policy"
  path   = local.iam_path
  policy = one(data.aws_iam_policy_document.codedeploy_lambda[*].json)
}

resource "aws_iam_role" "codedeploy" {
  name                  = "${local.name_prefix}-codedeploy"
  path                  = local.iam_path
  permissions_boundary  = local.permissions_boundary_arn
  assume_role_policy    = data.aws_iam_policy_document.codedeploy_assume.json
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "codedeploy" {
  for_each = merge({
    ecs   = aws_iam_policy.codedeploy_ecs.arn
    elbv2 = aws_iam_policy.codedeploy_elbv2.arn
    iam   = aws_iam_policy.codedeploy_iam.arn
  }, local.regression_wrapper_enabled ? { lambda = one(aws_iam_policy.codedeploy_lambda[*].arn) } : {})

  role       = aws_iam_role.codedeploy.name
  policy_arn = each.value
}
