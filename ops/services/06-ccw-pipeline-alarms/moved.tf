moved {
  from = aws_cloudwatch_log_group.verifier
  to   = module.log_group_verifier.aws_cloudwatch_log_group.this
}
