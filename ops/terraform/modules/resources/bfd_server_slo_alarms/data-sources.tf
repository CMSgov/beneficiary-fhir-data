data "aws_region" "current" {}

data "external" "client_ssls_by_partner" {
  for_each = local.metrics

  program = [
    "bash",
    "${path.module}/get-partner-client-ssl.sh",
    each.value,
    local.namespace,
    jsonencode({
      for partner, config in local.all_partners :
      partner => lookup(config.client_ssl_regex, replace(var.env, "-", "_"), null)
    })
  ]
}
