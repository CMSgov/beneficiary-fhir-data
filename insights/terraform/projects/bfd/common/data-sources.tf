# TODO: Replace when/if this is merged into main Terraform
data "aws_lambda_function" "server_regression_glue_triggers" {
  for_each      = local.envs
  function_name = "${local.project}-${each.key}-server-regression-glue-trigger"
}

data "aws_lambda_function" "bfd_insights_error_slack" {
  for_each      = local.envs
  function_name = "${local.project}-${each.key}-bfd-insights-error-slack"
}
