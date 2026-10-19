data "aws_iam_policy_document" "samhsa_hook_logs" {
  count = local.samhsa_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [for arn in module.log_group_samhsa_hook[*].arn : "${arn}:*"]
  }
}

resource "aws_iam_policy" "samhsa_hook_logs" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name = "${local.sh_lambda_full_name}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.sh_lambda_full_name} Lambda to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = one(data.aws_iam_policy_document.samhsa_hook_logs[*].json)
}

data "aws_iam_policy_document" "samhsa_hook_lambda" {
  count = local.samhsa_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowInvokeSamhsaRegressionLambda"
    actions   = ["lambda:InvokeFunction"]
    resources = aws_lambda_function.samhsa_regression[*].arn
  }
}

resource "aws_iam_policy" "samhsa_hook_lambda" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name        = "${local.sh_lambda_full_name}-lambda-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.sh_lambda_full_name} to invoke the ${one(aws_lambda_function.samhsa_regression[*].function_name)} Lambda"
  policy      = one(data.aws_iam_policy_document.samhsa_hook_lambda[*].json)
}

data "aws_iam_policy_document" "samhsa_hook_kms" {
  count = local.samhsa_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "samhsa_hook_kms" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name        = "${local.sh_lambda_full_name}-kms-policy"
  path        = local.iam_path
  description = "Grants permission for the ${local.sh_lambda_full_name} to use the ${local.env_key_alias} CMK"
  policy      = one(data.aws_iam_policy_document.samhsa_hook_kms[*].json)
}

resource "aws_iam_role" "samhsa_hook" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name                  = "${local.sh_lambda_full_name}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.sh_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "samhsa_hook" {
  for_each = local.samhsa_hook_enabled ? {
    logs   = one(aws_iam_policy.samhsa_hook_logs[*].arn)
    lambda = one(aws_iam_policy.samhsa_hook_lambda[*].arn)
    kms    = one(aws_iam_policy.samhsa_hook_kms[*].arn)
  } : {}

  role       = one(aws_iam_role.samhsa_hook[*].name)
  policy_arn = each.value
}

data "aws_iam_policy_document" "run_samhsa_hook_lambda" {
  count = local.samhsa_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowUsageOfSamhsaHookLambda"
    actions   = ["lambda:InvokeFunction", "lambda:GetFunction"]
    resources = aws_lambda_function.samhsa_hook[*].arn
  }
}

resource "aws_iam_policy" "run_samhsa_hook_lambda" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name   = "${local.name_prefix}-run-samhsa-hook-lambda-policy"
  path   = local.iam_path
  policy = one(data.aws_iam_policy_document.run_samhsa_hook_lambda[*].json)
}

resource "aws_iam_role" "run_samhsa_hook" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name                  = "${local.name_prefix}-run-samhsa-hook-role"
  path                  = local.iam_path
  description           = "Role for ${local.service} deploy hooks to run ${local.sh_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["ecs"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "run_samhsa_hook" {
  for_each = local.samhsa_hook_enabled ? {
    lambda = one(aws_iam_policy.run_samhsa_hook_lambda[*].arn)
  } : {}

  role       = one(aws_iam_role.run_samhsa_hook[*].name)
  policy_arn = each.value
}
