data "aws_iam_policy_document" "samhsa_regression_logs" {
  count = local.samhsa_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowLogStreamControl"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [for arn in module.log_group_samhsa_regression[*].arn : "${arn}:*"]
  }
}

resource "aws_iam_policy" "samhsa_regression_logs" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name = "${local.samhsa_regression_lambda_full_name}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.samhsa_regression_lambda_full_name} Lambda to write to its ",
    "corresponding CloudWatch Log Group and Log Streams"
  ])

  policy = one(data.aws_iam_policy_document.samhsa_regression_logs[*].json)
}

data "aws_iam_policy_document" "samhsa_regression_ssm" {
  count = local.samhsa_hook_enabled ? 1 : 0

  statement {
    sid     = "AllowAccessToUsedParams"
    actions = ["ssm:GetParameter"]
    resources = [
      for param in [
        "server-ng/sensitive/db/username",
        "server-ng/sensitive/db/password",
        "server-ng/sensitive/test_client_cert",
        "server-ng/sensitive/test_client_key",
        "server-ng/sensitive/test_client_samhsa_cert",
        "server-ng/sensitive/test_client_samhsa_key",
      ]
      : "arn:aws:ssm:${local.region}:${local.account_id}:parameter/bfd/${local.env}/${param}"
    ]
  }
}

resource "aws_iam_policy" "samhsa_regression_ssm" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name = "${local.samhsa_regression_lambda_full_name}-ssm-policy"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.samhsa_regression_lambda_full_name} Lambda to get the SSM ",
    "parameters it needs to run the SAMHSA regression suite"
  ])

  policy = one(data.aws_iam_policy_document.samhsa_regression_ssm[*].json)
}

data "aws_iam_policy_document" "samhsa_regression_kms" {
  count = local.samhsa_hook_enabled ? 1 : 0

  statement {
    sid       = "AllowEncryptAndDecryptWithEnvCmk"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey"]
    resources = [local.env_key_arn]
  }
}

resource "aws_iam_policy" "samhsa_regression_kms" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name = "${local.samhsa_regression_lambda_full_name}-kms-policy"
  path = local.iam_path
  description = join("", [
    "Grants permissions for the ${local.samhsa_regression_lambda_full_name} Lambda to use the ",
    "${local.env_key_alias} key"
  ])

  policy = one(data.aws_iam_policy_document.samhsa_regression_kms[*].json)
}

data "aws_iam_policy" "lambda_vpc_access_role" {
  name = "AWSLambdaVPCAccessExecutionRole"
}

resource "aws_iam_role" "samhsa_regression" {
  count = local.samhsa_hook_enabled ? 1 : 0

  name                  = "${local.samhsa_regression_lambda_full_name}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.samhsa_regression_lambda_full_name} Lambda"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["lambda"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "samhsa_regression" {
  for_each = local.samhsa_hook_enabled ? {
    logs       = one(aws_iam_policy.samhsa_regression_logs[*].arn)
    ssm        = one(aws_iam_policy.samhsa_regression_ssm[*].arn)
    kms        = one(aws_iam_policy.samhsa_regression_kms[*].arn)
    vpc_access = data.aws_iam_policy.lambda_vpc_access_role.arn
  } : {}

  role       = one(aws_iam_role.samhsa_regression[*].name)
  policy_arn = each.value
}
