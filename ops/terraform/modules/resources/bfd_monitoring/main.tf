locals {
  log_groups = {
    access = "/bfd/${var.env}/bfd-server/access.json"
  }
  namespace = "bfd-${var.env}/bfd-server"
  endpoints = {
    "all" : "*/fhir/*",
    "metadata" : "*/fhir/metadata*",
    "coverageAll" : "*/fhir/Coverage*",
    "patientAll" : "*/fhir/Patient*",
    "eobAll" : "*/fhir/ExplanationOfBenefit*",
  }
  partners = {
    "all" : "*",
    "bb" : "*BlueButton*",
    "bcda" : "*bcda*",
    "dpc" : "*dpc*",
    "ab2d" : "*ab2d*",
  }

  metric_config = flatten([
    for endpoint_key, endpoint_value in local.endpoints : [
      for partner_key, partner_value in local.partners : {
        name    = "${endpoint_key}/${partner_key}"
        pattern = "($.mdc.http_access_request_clientSSL_DN = \"${partner_value}\") && ($.mdc.http_access_request_uri = \"${endpoint_value}\")"
      }
    ]
  ])
}

# Count requests per endpoint, per partner
resource "aws_cloudwatch_log_metric_filter" "http-requests-count" {
  count          = length(local.metric_config)
  name           = "bfd-${var.env}/bfd-server/http-requests/count/${local.metric_config[count.index].name}"
  pattern        = "{ ${local.metric_config[count.index].pattern} }"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/${local.metric_config[count.index].name}"
    namespace     = local.namespace
    value         = "1"
    default_value = "0"
  }
}

# Latency per endpoint, per partner
resource "aws_cloudwatch_log_metric_filter" "http-requests-latency" {
  count          = length(local.metric_config)
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/${local.metric_config[count.index].name}"
  pattern        = "{${local.metric_config[count.index].pattern} && ($.mdc.http_access_response_duration_milliseconds = *)}"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/${local.metric_config[count.index].name}"
    namespace     = local.namespace
    value         = "$.mdc.http_access_response_duration_milliseconds"
    default_value = null
  }
}

# Count HTTP 500s per partner
resource "aws_cloudwatch_log_metric_filter" "http-requests-count-500" {
  for_each       = local.partners
  name           = "bfd-${var.env}/bfd-server/http-requests/count-500/${each.key}"
  pattern        = "{($.mdc.http_access_request_clientSSL_DN = \"${each.value}\") && ($.mdc.http_access_response_status = 500)}"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count-500/${each.key}"
    namespace     = local.namespace
    value         = "1"
    default_value = "0"
  }
}

# Count HTTP non-2XXs per partner
resource "aws_cloudwatch_log_metric_filter" "http-requests-count-not-2xx" {
  for_each       = local.partners
  name           = "bfd-${var.env}/bfd-server/http-requests/count-not-2xx/${each.key}"
  pattern        = "{($.mdc.http_access_request_clientSSL_DN = \"${each.value}\") && ($.mdc.http_access_response_status != 200)}"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count-not-2xx/${each.key}"
    namespace     = local.namespace
    value         = "1"
    default_value = "0"
  }
}

# BFD Server Dashboard
resource "aws_cloudwatch_dashboard" "bfd-server-dashboard" {
  dashboard_name = "bfd-server-${var.env}"
  dashboard_body = templatefile("${path.module}/templates/bfd-server-dashboard.tpl", {
    dashboard_namespace = local.namespace
    asg_name            = var.asg_id
  })
}
