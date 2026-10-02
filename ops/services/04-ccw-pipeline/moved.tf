moved {
  from = aws_cloudwatch_log_group.ccw_runner
  to   = module.log_group_ccw_runner.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.ccw_messages
  to   = module.log_group_ccw_messages.aws_cloudwatch_log_group.this
}
