moved {
  from = aws_glue_crawler.glue-crawler-api-requests
  to   = aws_glue_crawler.glue_crawler_api_requests
}

moved {
  from = aws_iam_policy.iam-policy-firehose
  to   = aws_iam_policy.iam_policy_firehose
}

moved {
  from = aws_iam_role.iam-role-cloudwatch-logs
  to   = aws_iam_role.iam_role_cloudwatch_logs
}

moved {
  from = aws_iam_role.iam-role-firehose
  to   = aws_iam_role.iam_role_firehose
}

moved {
  from = aws_iam_role.iam-role-firehose-lambda
  to   = aws_iam_role.iam_role_firehose_lambda
}

moved {
  from = aws_kinesis_firehose_delivery_stream.firehose-ingester
  to   = aws_kinesis_firehose_delivery_stream.firehose_ingester
}

moved {
  from = aws_lambda_function.lambda-function-format-firehose-logs
  to   = aws_lambda_function.lambda_function_format_firehose_logs
}

moved {
  from = aws_cloudwatch_log_subscription_filter.cloudwatch-access-log-subscription
  to   = aws_cloudwatch_log_subscription_filter.cloudwatch_access_log_subscription
}

moved {
  from = module.glue-table-api-requests
  to   = module.glue_table_api_requests
}

moved {
  from = module.athena_views.null_resource.athena_view_api_requests_by_bene
  to   = null_resource.athena_view_api_requests_by_bene
}

moved {
  from = module.athena_views.null_resource.athena_view_api_requests
  to   = null_resource.athena_view_api_requests
}

moved {
  from = module.athena_views.null_resource.athena_view_new_benes_by_day
  to   = null_resource.athena_view_new_benes_by_day
}
