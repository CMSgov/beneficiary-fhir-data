data "aws_iam_policy_document" "locust_hook_logs" {
  count = local.locust_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [for arn in module.log_group_locust_hook[*].arn : "${arn}:*"]
  }
}

resource "aws_iam_policy" "locust_hook_logs" {
  count = local.locust_hook_enabled ? 1 : 0

  name = "${local.lh_lambda_full_name}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.lh_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = one(data.aws_iam_policy_document.locust_hook_logs[*].json)
}

data "aws_iam_policy_document" "locust_hook_lambda" {
  count = local.locust_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowInvokeRunLocustLambda"
    actions   = ["lambda:InvokeFunction"]
    resources = data.aws_lambda_function.run_locust[*].arn
  }
}

resource "aws_iam_policy" "locust_hook_lambda" {
  count = local.locust_hook_enabled ? 1 : 0

  name        = "${local.lh_lambda_full_name}-lambda-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.lh_lambda_full_name} to invoke the ${one(data.aws_lambda_function.run_locust[*].function_name)} Lambda"
  policy      = one(data.aws_iam_policy_document.locust_hook_lambda[*].json)
}

data "aws_iam_policy_document" "locust_hook_kms" {
  count = local.locust_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "locust_hook_kms" {
  count = local.locust_hook_enabled ? 1 : 0

  name        = "${local.lh_lambda_full_name}-kms-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.lh_lambda_full_name} to use the ${local.env_key_alias} CMK"
  policy      = one(data.aws_iam_policy_document.locust_hook_kms[*].json)
}

resource "aws_iam_role" "locust_hook" {
  count = local.locust_hook_enabled ? 1 : 0

  name                  = "${local.lh_lambda_full_name}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.lh_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "locust_hook" {
  for_each = local.locust_hook_enabled ? {
    logs   = one(aws_iam_policy.locust_hook_logs[*].arn)
    lambda = one(aws_iam_policy.locust_hook_lambda[*].arn)
    kms    = one(aws_iam_policy.locust_hook_kms[*].arn)
  } : {}

  role       = one(aws_iam_role.locust_hook[*].name)
  policy_arn = each.value
}

data "aws_iam_policy_document" "run_locust_hook_lambda" {
  count = local.locust_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowUsageOfLocustHookLambda"
    actions   = ["lambda:InvokeFunction", "lambda:GetFunction"]
    resources = aws_lambda_function.locust_hook[*].arn
  }
}

resource "aws_iam_policy" "run_locust_hook_lambda" {
  count = local.locust_hook_enabled ? 1 : 0

  name   = "${local.name_prefix}-run-locust-hook-lambda-policy"
  path   = local.iam_path
  policy = one(data.aws_iam_policy_document.run_locust_hook_lambda[*].json)
}

resource "aws_iam_role" "run_locust_hook" {
  count = local.locust_hook_enabled ? 1 : 0

  name                  = "${local.name_prefix}-run-locust-hook-role"
  path                  = local.iam_path
  description           = "Role for ${local.service} deploy hooks to run ${local.lh_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "run_locust_hook" {
  for_each = local.locust_hook_enabled ? {
    lambda = one(aws_iam_policy.run_locust_hook_lambda[*].arn)
  } : {}

  role       = one(aws_iam_role.run_locust_hook[*].name)
  policy_arn = each.value
}
