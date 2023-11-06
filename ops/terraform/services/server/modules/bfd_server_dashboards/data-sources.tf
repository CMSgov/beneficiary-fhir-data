data "aws_region" "current" {}

data "external" "client_ssls" {
  program = [
    "bash",
    "${path.module}/get-partner-client-ssl.sh",
    local.all_requests_count_metric,
    local.namespace,
    jsonencode({
      for partner, per_env_regex in local.partner_client_ssl_regexs :
      partner => lookup(per_env_regex, replace(local.env, "-", "_"), null)
    })
  ]
}
