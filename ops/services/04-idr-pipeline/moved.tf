moved {
  from = aws_cloudwatch_log_group.events
  to   = module.log_group_events.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.idr_messages
  to   = module.log_group_idr_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.run_idr
  to   = module.log_group_run_idr.aws_cloudwatch_log_group.this
}
