data "aws_region" "current" {}

data "aws_sns_topic" "high_alert_sns" {
  count = local.env_sns.high_alert != "null" ? 1 : 0
  name  = local.env_sns.high_alert
}

data "aws_sns_topic" "alert_sns" {
  count = local.env_sns.alert != "null" ? 1 : 0
  name  = local.env_sns.alert
}

data "aws_sns_topic" "warning_sns" {
  count = local.env_sns.warning != "null" ? 1 : 0
  name  = local.env_sns.warning
}

data "external" "client_ssls_by_partner" {
  for_each = local.metrics

  program = [
    "bash",
    "${path.module}/get-partner-client-ssl.sh",
    each.value,
    local.namespace,
    jsonencode({
      for partner, config in local.all_partners :
      partner => lookup(config.client_ssl_regex, replace(local.env, "-", "_"), null)
    })
  ]
}
