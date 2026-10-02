moved {
  from = aws_cloudwatch_log_group.regression_wrapper[0]
  to   = module.log_group_regression_wrapper[0].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.certstores_messages
  to   = module.log_group_certstores_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.log_router_messages
  to   = module.log_group_log_router_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.server_messages
  to   = module.log_group_server_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.server_access
  to   = module.log_group_server_access.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.adot_messages
  to   = module.log_group_adot_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.adot_metrics
  to   = module.log_group_adot_metrics.aws_cloudwatch_log_group.this
}
