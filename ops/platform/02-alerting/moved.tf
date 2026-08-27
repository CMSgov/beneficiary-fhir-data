moved {
  from = aws_cloudwatch_log_group.slack_alerter
  to   = module.log_group_slack_alerter.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.splunk_incident_success
  to   = module.log_group_splunk_incident_success.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.splunk_incident_failure
  to   = module.log_group_splunk_incident_failure.aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_success["bfd-warnings"]
  to   = module.log_group_slack_success["bfd-warnings"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_success["bfd-notices"]
  to   = module.log_group_slack_success["bfd-notices"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_success["bfd-internal-alerts"]
  to   = module.log_group_slack_success["bfd-internal-alerts"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_success["bfd-alerts"]
  to   = module.log_group_slack_success["bfd-alerts"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_failure["bfd-warnings"]
  to   = module.log_group_slack_failure["bfd-warnings"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_failure["bfd-notices"]
  to   = module.log_group_slack_failure["bfd-notices"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_failure["bfd-internal-alerts"]
  to   = module.log_group_slack_failure["bfd-internal-alerts"].aws_cloudwatch_log_group.this
}

moved {
  from = aws_cloudwatch_log_group.slack_failure["bfd-alerts"]
  to   = module.log_group_slack_failure["bfd-alerts"].aws_cloudwatch_log_group.this
}

