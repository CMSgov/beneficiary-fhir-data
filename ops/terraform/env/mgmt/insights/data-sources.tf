data "aws_lambda_function" "server_regression_glue_triggers" {
  for_each      = local.envs
  function_name = "${local.project}-${each.key}-server-regression-glue-trigger"
}

data "aws_lambda_function" "bfd_insights_error_slack" {
  for_each      = local.envs
  function_name = "${local.project}-${each.key}-bfd-insights-error-slack"
}

data "aws_lambda_function" "bfd_insights_trigger_glue_crawler" {
  for_each      = local.envs
  function_name = "${local.project}-insights-${local.project}-${each.key}-trigger-glue-crawler"
}
