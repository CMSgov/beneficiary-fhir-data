resource "aws_cloudwatch_event_rule" "rule_schedule" {
  name                = var.name
  description         = var.description
  schedule_expression = var.schedule
}
