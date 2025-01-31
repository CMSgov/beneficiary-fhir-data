data "aws_region" "current" {}

data "aws_sns_topic" "alert_sns" {
  count = local.alert_sns_name != null ? 1 : 0
  name  = local.alert_sns_name
}

data "aws_sns_topic" "warning_sns" {
  count = local.warning_sns_name != null ? 1 : 0
  name  = local.warning_sns_name
}

data "aws_sns_topic" "alert_ok_sns" {
  count = local.alert_ok_sns_name != null ? 1 : 0
  name  = local.alert_ok_sns_name
}

data "aws_sns_topic" "warning_ok_sns" {
  count = local.warning_ok_sns_name != null ? 1 : 0
  name  = local.warning_ok_sns_name
}
