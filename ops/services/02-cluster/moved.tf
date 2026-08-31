moved {
  from = aws_cloudwatch_log_group.this
  to   = module.log_group_this.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.ecs_events[0]
  to   = module.log_group_ecs_events[0].aws_cloudwatch_log_group.this
}

