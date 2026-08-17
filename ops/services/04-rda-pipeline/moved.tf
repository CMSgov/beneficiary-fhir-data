moved {
  from = aws_cloudwatch_log_group.rda_messages
  to   = module.log_group_rda_messages.aws_cloudwatch_log_group.this
}
