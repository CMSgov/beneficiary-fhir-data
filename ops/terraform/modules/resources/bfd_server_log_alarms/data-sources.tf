data "aws_sns_topic" "alert_sns" {
  count = local.alert_sns_name != null ? 1 : 0
  name  = local.alert_sns_name
}

data "aws_sns_topic" "notify_sns" {
  count = local.notify_sns_name != null ? 1 : 0
  name  = local.notify_sns_name
}

data "aws_sns_topic" "ok_sns" {
  count = local.ok_sns_name != null ? 1 : 0
  name  = local.ok_sns_name
}
