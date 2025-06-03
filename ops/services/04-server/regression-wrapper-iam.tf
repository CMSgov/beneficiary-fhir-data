data "aws_iam_policy_document" "regression_wrapper_logs" {
  count = local.regression_wrapper_enabled ? 1 : 0

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [for arn in aws_cloudwatch_log_group.regression_wrapper[*].arn : "${arn}:*"]
  }
}

resource "aws_iam_policy" "regression_wrapper_logs" {
  count = local.regression_wrapper_enabled ? 1 : 0

  name = "${local.rw_lambda_full_name}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.rw_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = one(data.aws_iam_policy_document.regression_wrapper_logs[*].json)
}

data "aws_iam_policy_document" "regression_wrapper_lambda" {
  count = local.regression_wrapper_enabled ? 1 : 0

  statement {
    sid       = "AllowInvokeRunLocustLambda"
    actions   = ["lambda:InvokeFunction"]
    resources = data.aws_lambda_function.run_locust[*].arn
  }
}

resource "aws_iam_policy" "regression_wrapper_lambda" {
  count = local.regression_wrapper_enabled ? 1 : 0

  name        = "${local.rw_lambda_full_name}-lambda-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.rw_lambda_full_name} to invoke the ${one(data.aws_lambda_function.run_locust[*].function_name)} Lambda"
  policy      = one(data.aws_iam_policy_document.regression_wrapper_lambda[*].json)
}

data "aws_iam_policy_document" "regression_wrapper_kms" {
  count = local.regression_wrapper_enabled ? 1 : 0

  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "regression_wrapper_kms" {
  count = local.regression_wrapper_enabled ? 1 : 0

  name        = "${local.rw_lambda_full_name}-kms-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.rw_lambda_full_name} to use the ${local.env_key_alias} CMK"
  policy      = one(data.aws_iam_policy_document.regression_wrapper_kms[*].json)
}

data "aws_iam_policy_document" "regression_wrapper_codedeploy" {
  count = local.regression_wrapper_enabled ? 1 : 0

  statement {
    sid     = "AllowPutLifecycleEventHookStatus"
    actions = ["codedeploy:PutLifecycleEventHookExecutionStatus"]
    # Action does not support any restrictions
    # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_awscodedeploy.html
    resources = ["*"]
  }
}

resource "aws_iam_policy" "regression_wrapper_codedeploy" {
  count = local.regression_wrapper_enabled ? 1 : 0

  name        = "${local.rw_lambda_full_name}-codedeploy-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.rw_lambda_full_name} to put a Lifecycle Event Hook status for a deployment"
  policy      = one(data.aws_iam_policy_document.regression_wrapper_codedeploy[*].json)
}

data "aws_iam_policy_document" "lambda_assume_regression_wrapper" {
  count = local.regression_wrapper_enabled ? 1 : 0

  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "regression_wrapper" {
  count = local.regression_wrapper_enabled ? 1 : 0

  name                  = "${local.rw_lambda_full_name}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.rw_lambda_full_name} Lambda"
  assume_role_policy    = one(data.aws_iam_policy_document.lambda_assume_regression_wrapper[*].json)
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "regression_wrapper" {
  for_each = local.regression_wrapper_enabled ? {
    logs       = one(aws_iam_policy.regression_wrapper_logs[*].arn)
    lambda     = one(aws_iam_policy.regression_wrapper_lambda[*].arn)
    kms        = one(aws_iam_policy.regression_wrapper_kms[*].arn)
    codedeploy = one(aws_iam_policy.regression_wrapper_codedeploy[*].arn)
  } : {}

  role       = one(aws_iam_role.regression_wrapper[*].name)
  policy_arn = each.value
}
