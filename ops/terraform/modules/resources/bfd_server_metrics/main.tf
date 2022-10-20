locals {
  log_groups = {
    access = "/bfd/${var.env}/bfd-server/access.json"
  }
  namespace = "bfd-${var.env}/bfd-server"
  endpoints = {
    all                = "*/fhir/*"
    metadata           = "*/fhir/metadata"
    coverage_all       = "*/fhir/Coverage"
    patient_all        = "*/fhir/Patient"
    eob_all            = "*/fhir/ExplanationOfBenefit"
    claim_all          = "*/fhir/Claim"
    claim_response_all = "*/fhir/ClaimResponse"
  }

  endpoint_patterns = {
    for name, pattern in local.endpoints :
    replace(name, "_", "-") => "$.mdc.http_access_request_uri = \"${pattern}\""
  }

  client_ssl_pattern = "$.mdc.http_access_request_clientSSL_DN = \"*\""

  filter_variations = {
    all_partners = {
      name_suffix = "all"
      dimensions  = null
    }
    by_partner = {
      name_suffix = "by-partner"
      dimensions = {
        client_ssl = "$.mdc.http_access_request_clientSSL_DN"
      }
    }
  }

  endpoint_filters_config = merge([
    for endpoint_key, endpoint_pattern in local.endpoint_patterns : {
      for variation, variation_config in local.filter_variations :
      "${replace(endpoint_key, "-", "_")}_${variation}" => {
        "resource_name"    = endpoint_key
        "endpoint_pattern" = endpoint_pattern
        "name_suffix"      = variation_config.name_suffix
        "dimensions"       = variation_config.dimensions
      }
    }
  ]...)
}

# Count requests per endpoint with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count" {
  for_each = local.endpoint_filters_config

  name           = "bfd-${var.env}/bfd-server/http-requests/count/${each.value.resource_name}/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${each.value.endpoint_pattern} && ",
    "${local.client_ssl_pattern}}",
  ])

  metric_transformation {
    name       = "http-requests/count/${each.value.resource_name}"
    namespace  = local.namespace
    value      = "1"
    dimensions = each.value.dimensions
  }
}

# Latency per endpoint with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency" {
  for_each = local.endpoint_filters_config

  name           = "bfd-${var.env}/bfd-server/http-requests/latency/${each.value.resource_name}/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${each.value.endpoint_pattern} && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/${each.value.resource_name}"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
  }
}

# Count HTTP 5xxs with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_500_responses" {
  for_each = local.filter_variations

  name           = "bfd-${var.env}/bfd-server/http-requests/count/500-responses/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_response_status = 500 && ",
    "${local.client_ssl_pattern}}",
  ])
  metric_transformation {
    name       = "http-requests/count/500-responses"
    namespace  = local.namespace
    value      = "1"
    dimensions = each.value.dimensions
  }
}

# Count HTTP non-2XXs with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_non_2xx_responses" {
  for_each = local.filter_variations

  name           = "bfd-${var.env}/bfd-server/http-requests/count/non-2xx-responses/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_response_status != 200 && ",
    "${local.client_ssl_pattern}}",
  ])

  metric_transformation {
    name       = "http-requests/count/non-2xx-responses"
    namespace  = local.namespace
    value      = "1"
    dimensions = each.value.dimensions
  }
}

# Latency for Patient endpoints _not_ by contract (for SLOs) with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_patient_not_by_contract" {
  for_each = local.filter_variations

  name           = "bfd-${var.env}/bfd-server/http-requests/latency/patient-not-by-contract/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  # Terraform HCL has no support for breaking long strings, so the join() function is used as a
  # poor, but functional, substitute. Otherwise this pattern would be far too long
  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.patient_all}\" && ",
    "$.mdc.http_access_request_operation != \"*by=*contract*\" && ",
    "$.mdc.http_access_request_operation != \"*by=*Contract*\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/patient-not-by-contract"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
  }
}

# Latency for Patient endpoints by contract with count = 4000 (for SLOs) with partner client SSL as
# dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_patient_by_contract_count_4000" {
  for_each = local.filter_variations

  name           = "bfd-${var.env}/bfd-server/http-requests/latency/patient-by-contract-count-4000/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.patient_all}\" && ",
    "$.mdc.http_access_request_query_string = \"*_count=4000*\" && ",
    "($.mdc.http_access_request_operation = \"*by=*contract*\" || ",
    "$.mdc.http_access_request_operation = \"*by=*Contract*\") && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/patient-by-contract-count-4000"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
  }
}

# Latency per-KB for all EoB endpoints (for SLOs) with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_by_kb_eob_all" {
  for_each = local.filter_variations

  name           = "bfd-${var.env}/bfd-server/http-requests/latency-by-kb/eob-all/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.eob_all}\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.http_access_response_duration_per_kb = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency-by-kb/eob-all"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_per_kb"
    dimensions = each.value.dimensions
  }
}

# Latency for all EoB endpoints with no resources returned (for SLOs) with partner client SSL as
# dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_eob_all_no_resources" {
  for_each = local.filter_variations

  name           = "bfd-${var.env}/bfd-server/http-requests/latency/eob-all-no-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.eob_all}\" && ",
    "$.mdc.resources_returned_count = 0 && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/eob-all-no-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
  }
}

# This metric filter is deprecrated, but an existing CloudWatch alarm (bfd-${env}-server-all-500s)
# depends on it.
# TODO: Remove this metric filter in BFD-1773
resource "aws_cloudwatch_log_metric_filter" "deprecated_http_requests_count_500" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count-500/all"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{($.mdc.http_access_request_clientSSL_DN = \"*\") && ",
    "($.mdc.http_access_response_status = 500)}"
  ])

  metric_transformation {
    name          = "http-requests/count-500/all"
    namespace     = local.namespace
    value         = "1"
    default_value = "0"
  }
}

# This metric filter is deprecrated, but an existing CloudWatch alarm
# (bfd-${env}-server-all-eob-6s-p95) depends on it. 
# TODO: Remove this metric filter in BFD-1773
resource "aws_cloudwatch_log_metric_filter" "deprecated_http_requests_latency_eob_all" {
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/eobAll/all"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{($.mdc.http_access_request_clientSSL_DN = \"*\") && ",
    "($.mdc.http_access_request_uri = \"${local.endpoints.eob_all}\") && ",
    "($.mdc.http_access_response_duration_milliseconds = *)}"
  ])

  metric_transformation {
    name          = "http-requests/latency/eobAll/all"
    namespace     = local.namespace
    value         = "$.mdc.http_access_response_duration_milliseconds"
    default_value = null
  }
}