moved {
  from = aws_cloudwatch_log_group.checker
  to   = module.log_group_checker.aws_cloudwatch_log_group.this
}
