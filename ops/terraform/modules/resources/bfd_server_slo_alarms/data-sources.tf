data "external" "metrics_by_partner" {
  for_each = local.metrics

  program = [
    "bash", 
    "get-partner-metrics.sh", 
    each.value, 
    local.namespace, 
    jsonencode(local.partner_regexs)
  ]
}