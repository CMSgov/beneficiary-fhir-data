locals {
  log_groups = {
    access = "/bfd/${var.env}/bfd-server/access.json"
  }
  namespace = "bfd-${var.env}/bfd-server"
  endpoints = {
    all                 = "*/fhir/*"
    metadata            = "*/fhir/metadata"
    coverageAll         = "*/fhir/Coverage"
    patientAll          = "*/fhir/Patient"
    eobAll              = "*/fhir/ExplanationOfBenefit"
    claimAll            = "*/fhir/Claim"
    claimResponseAll    = "*/fhir/ClaimResponse"
  }

  partners = {
    all  = "*"
    bb   = "*BlueButton*"
    bcda = "*bcda*"
    dpc  = "*dpc*"
    ab2d = "*ab2d*"
    test = "*data-server-client-test*"
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

# Metric Filters Configured with new json format
# Count requests per endpoint, per partner
resource "aws_cloudwatch_log_metric_filter" "http_requests_count" {
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
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency" {
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
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_500" {
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
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_not_2xx" {
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

# Latency per-partner for Patient endpoints _not_ by contract (for SLOs)
resource "aws_cloudwatch_log_metric_filter" "http_requests_patient_not_by_contract_latency" {
  for_each       = local.partners
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/patientNotByContract/${each.key}"
  # Terraform HCL has no support for breaking long strings, so the join() function is used as a
  # poor, but functional, substitute. Otherwise this pattern would be far too long
  pattern        = join("", "{$.mdc.http_access_request_clientSSL_DN = \"${each.value}\" &&",
                            " $.mdc.http_access_request_uri = \"${local.endpoints.patientAll}\" &&",
                            " $.mdc.http_access_request_operation != \"*by=*contract*\" &&",
                            " $.mdc.http_access_request_operation != \"*by=*Contract*\" &&",
                            " $.mdc.http_access_response_duration_milliseconds = *}")
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/patientNotByContract/${each.key}"
    namespace     = local.namespace
    value         = "$.mdc.http_access_response_duration_milliseconds"
    default_value = null
  }
}

# Latency per-partner for Patient endpoints by contract with count = 4000 (for SLOs)
resource "aws_cloudwatch_log_metric_filter" "http_requests_patient_by_contract_count_4000_latency" {
  for_each       = local.partners
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/patientByContractCount4000/${each.key}"
  # Terraform HCL has no support for breaking long strings, so the join() function is used as a
  # poor, but functional, substitute. Otherwise this pattern would be far too long
  pattern        = join("", "{$.mdc.http_access_request_clientSSL_DN = \"${each.value}\" &&",
                            " $.mdc.http_access_request_uri = \"${local.endpoints.patientAll}\" &&",
                            " $.mdc.http_access_request_query_string = \"*_count=4000*\" &&",
                            " ($.mdc.http_access_request_operation = \"*by=*contract*\" ||",
                            " $.mdc.http_access_request_operation = \"*by=*Contract*\") &&",
                            " $.mdc.http_access_response_duration_milliseconds = *}")
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/patientByContractCount4000/${each.key}"
    namespace     = local.namespace
    value         = "$.mdc.http_access_response_duration_milliseconds"
    default_value = null
  }
}

# Latency per-KB for all EoB endpoints (for SLOs)
resource "aws_cloudwatch_log_metric_filter" "http_requests_eob_all_latency_by_kb" {
  for_each       = local.partners
  name           = "bfd-${var.env}/bfd-server/http-requests/latencyByKB/eobAll/${each.key}"
  # Terraform HCL has no support for breaking long strings, so the join() function is used as a
  # poor, but functional, substitute. Otherwise this pattern would be far too long
  pattern        = join("", "{$.mdc.http_access_request_clientSSL_DN = \"${each.value}\" &&",
                            " $.mdc.http_access_request_uri = \"${local.endpoints.eobAll}\" &&",
                            " $.mdc.http_access_response_duration_per_kb = *}")
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latencyByKB/eobAll/${each.key}"
    namespace     = local.namespace
    value         = "$.mdc.http_access_response_duration_per_kb"
    default_value = null
  }
}