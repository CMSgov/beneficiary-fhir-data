moved {
  from = aws_cloudwatch_log_group.this[0]
  to   = module.log_group_this[0].aws_cloudwatch_log_group.this
}
