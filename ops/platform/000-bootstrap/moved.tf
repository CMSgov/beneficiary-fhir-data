moved {
  from = aws_cloudwatch_log_group.runner["lambda"]
  to   = module.log_group_runner["lambda"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.runner["docker"]
  to   = module.log_group_runner["docker"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.runner["small"]
  to   = module.log_group_runner["small"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.runner["large"]
  to   = module.log_group_runner["large"].aws_cloudwatch_log_group.this
}
