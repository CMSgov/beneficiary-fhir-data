data "external" "metrics_by_partner" {
  for_each = local.metrics

  program = [
    "bash",
    "get-partner-metrics.sh",
    each.value,
    local.namespace,
    jsonencode({
      for partner, config in merge(local.partners.bulk, local.partners.non_bulk) :
      partner => config.client_ssl_regex[var.env]
    })
  ]
}