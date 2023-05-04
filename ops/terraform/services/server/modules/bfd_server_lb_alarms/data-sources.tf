# cloudwatch topics
data "aws_sns_topic" "cloudwatch_alarms" {
  name = "bfd-${local.env}-cloudwatch-alarms"
}
data "aws_sns_topic" "cloudwatch_ok" {
  name = "bfd-${local.env}-cloudwatch-ok"
}
