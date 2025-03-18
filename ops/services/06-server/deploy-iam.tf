data "aws_iam_policy_document" "codedeploy_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["codedeploy.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "codedeploy" {
  name                  = "${local.name_prefix}-codedeploy"
  path                  = local.cloudtamer_iam_path
  permissions_boundary  = data.aws_iam_policy.permissions_boundary.arn
  assume_role_policy    = data.aws_iam_policy_document.codedeploy_assume.json
  force_detach_policies = true
}

data "aws_iam_policy_document" "codedeploy" {
  statement {
    sid    = "AllowECSServiceModification"
    effect = "Allow"

    actions = [
      "ecs:DescribeServices",
      "ecs:UpdateServicePrimaryTaskSet",
    ]

    resources = [aws_ecs_service.server.id]
  }

  statement {
    sid = "AllowECSServiceTaskSetModification"
    actions = [
      "ecs:CreateTaskSet",
      "ecs:DeleteTaskSet",
    ]
    effect = "Allow"

    resources = ["${replace(aws_ecs_service.server.id, "service/", "task-set/")}/*"]
  }

  statement {
    sid    = "AllowELBDescribe"
    effect = "Allow"

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
    sid    = "AllowELBListenerModification"
    effect = "Allow"

    actions = [
      "elasticloadbalancing:ModifyListener",
      "elasticloadbalancing:ModifyRule"
    ]

    resources = [for _, v in aws_lb_listener.this : v.arn]
  }

  statement {
    sid    = "AllowELBListenerRuleModification"
    effect = "Allow"

    actions = [
      "elasticloadbalancing:ModifyRule"
    ]

    resources = [for _, v in aws_lb_listener.this : "${replace(v.arn, "listener/", "listener-rule/")}/*"]
  }

  statement {
    sid    = "AllowPassRole"
    effect = "Allow"

    actions = ["iam:PassRole"]

    resources = [
      aws_iam_role.service_role.arn,
      aws_iam_role.execution_role.arn
    ]
  }

  statement {
    sid    = "AllowUsageOfRegressionWrapperLambda"
    effect = "Allow"

    actions = ["lambda:InvokeFunction", "lambda:GetFunction"]

    resources = [aws_lambda_function.regression_wrapper.arn]
  }
}

resource "aws_iam_policy" "codedeploy" {
  name   = "${local.name_prefix}-codedeploy-policy"
  path   = local.cloudtamer_iam_path
  policy = data.aws_iam_policy_document.codedeploy.json
}

resource "aws_iam_role_policy_attachment" "codedeploy" {
  policy_arn = aws_iam_policy.codedeploy.arn
  role       = aws_iam_role.codedeploy.name
}
