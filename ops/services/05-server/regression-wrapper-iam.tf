data "aws_iam_policy_document" "regression_wrapper_logs" {
  statement {
    sid       = "AllowLogGroupCreate"
    actions   = ["logs:CreateLogGroup"]
    resources = ["arn:aws:logs:${local.region}:${local.account_id}:*"]
  }

  statement {
    sid     = "AllowLogStreamControl"
    actions = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [
      "arn:aws:logs:${local.region}:${local.account_id}:log-group:/aws/lambda/${local.rw_lambda_full_name}:*"
    ]
  }
}

resource "aws_iam_policy" "regression_wrapper_logs" {
  name = "${local.rw_lambda_full_name}-logs-policy"
  path = local.cloudtamer_iam_path
  description = join("", [
    "Permissions for the ${local.rw_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = data.aws_iam_policy_document.regression_wrapper_logs.json
}

data "aws_iam_policy_document" "regression_wrapper_lambda" {
  statement {
    sid       = "AllowInvokeRunLocustLambda"
    actions   = ["lambda:InvokeFunction"]
    resources = [data.aws_lambda_function.run_locust.arn]
  }
}

resource "aws_iam_policy" "regression_wrapper_lambda" {
  name        = "${local.rw_lambda_full_name}-lambda-policy"
  path        = local.cloudtamer_iam_path
  description = "Grants permission for the ${local.rw_lambda_full_name} to invoke the ${data.aws_lambda_function.run_locust.function_name} Lambda"
  policy      = data.aws_iam_policy_document.regression_wrapper_lambda.json
}

data "aws_iam_policy_document" "regression_wrapper_kms" {
  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [data.aws_kms_alias.env_cmk.target_key_arn]
  }
}

resource "aws_iam_policy" "regression_wrapper_kms" {
  name        = "${local.rw_lambda_full_name}-kms-policy"
  path        = local.cloudtamer_iam_path
  description = "Grants permission for the ${local.rw_lambda_full_name} to use the ${data.aws_kms_alias.env_cmk.name} CMK"
  policy      = data.aws_iam_policy_document.regression_wrapper_kms.json
}

data "aws_iam_policy_document" "regression_wrapper_codedeploy" {
  statement {
    sid     = "AllowPutLifecycleEventHookStatus"
    actions = ["codedeploy:PutLifecycleEventHookExecutionStatus"]
    # Action does not support any restrictions
    # See https://docs.aws.amazon.com/service-authorization/latest/reference/list_awscodedeploy.html
    resources = ["*"]
  }
}

resource "aws_iam_policy" "regression_wrapper_codedeploy" {
  name        = "${local.rw_lambda_full_name}-codedeploy-policy"
  path        = local.cloudtamer_iam_path
  description = "Grants permission for the ${local.rw_lambda_full_name} to put a Lifecycle Event Hook status for a deployment"
  policy      = data.aws_iam_policy_document.regression_wrapper_codedeploy.json
}

data "aws_iam_policy_document" "lambda_assume_regression_wrapper" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "regression_wrapper" {
  name                  = "${local.rw_lambda_full_name}-role"
  path                  = local.cloudtamer_iam_path
  description           = "Role for the ${local.rw_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.lambda_assume_regression_wrapper.json
  permissions_boundary  = data.aws_iam_policy.permissions_boundary.arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "regression_wrapper" {
  for_each = {
    logs       = aws_iam_policy.regression_wrapper_logs.arn
    lambda     = aws_iam_policy.regression_wrapper_lambda.arn
    kms        = aws_iam_policy.regression_wrapper_kms.arn
    codedeploy = aws_iam_policy.regression_wrapper_codedeploy.arn
  }

  role       = aws_iam_role.regression_wrapper.name
  policy_arn = each.value
}
