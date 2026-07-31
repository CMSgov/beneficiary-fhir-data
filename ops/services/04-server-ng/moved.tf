moved {
  from = aws_cloudwatch_log_group.s3logs
  to   = module.log_group_s3logs.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.log_router_messages
  to   = module.log_group_log_router_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.service_connect_messages
  to   = module.log_group_service_connect_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.server_messages
  to   = module.log_group_server_messages.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.server_healthchecks
  to   = module.log_group_server_healthchecks.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.server_nonjson
  to   = module.log_group_server_nonjson.aws_cloudwatch_log_group.this
}
