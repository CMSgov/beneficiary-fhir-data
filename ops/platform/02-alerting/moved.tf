moved {
  from = aws_cloudwatch_log_group.slack_alerter
  to   = module.log_group_slack_alerter.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.splunk_incident_success
  to   = module.log_group_splunk_incident_success.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_success
  to   = module.log_group_slack_success.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_failure
  to   = module.log_group_slack_failure.aws_cloudwatch_log_group.this
}

