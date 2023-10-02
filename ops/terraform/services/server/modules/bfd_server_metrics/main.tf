locals {
  env = terraform.workspace

  log_groups = {
    access   = "/bfd/${local.env}/bfd-server/access.json"
    messages = "/bfd/${local.env}/bfd-server/messages.json"
  }
  namespace = "bfd-${local.env}/bfd-server"
  endpoints = {
    all                = ["*/fhir/*"]
    metadata           = ["*/fhir/metadata*"]
    coverage_all       = ["*/fhir/Coverage*"]
    patient_all        = ["*/fhir/Patient*"]
    eob_all            = ["*/fhir/ExplanationOfBenefit*"]
    claim_all          = ["*/fhir/Claim", "*/fhir/Claim/"]
    claim_response_all = ["*/fhir/ClaimResponse", "*/fhir/ClaimResponse/"]
  }

  endpoint_patterns = {
    for name, patterns in local.endpoints :
    replace(name, "_", "-") => "(${join(" || ", [for pattern in patterns : "$.mdc.http_access_request_uri = \"${pattern}\""])})"
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

  name           = "bfd-${local.env}/bfd-server/http-requests/count/${each.value.resource_name}/${each.value.name_suffix}"
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
    unit       = "Count"
  }
}

# Latency per endpoint with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency" {
  for_each = local.endpoint_filters_config

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/${each.value.resource_name}/${each.value.name_suffix}"
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
    unit       = "Milliseconds"
  }
}

# Count HTTP 5xxs with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_500_responses" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/count/500-responses/${each.value.name_suffix}"
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
    unit       = "Count"
  }
}

# Count HTTP non-2XXs with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_non_2xx_responses" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/count/non-2xx-responses/${each.value.name_suffix}"
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
    unit       = "Count"
  }
}

# Latency for Patient endpoints _not_ by contract (for SLOs) with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_patient_not_by_contract" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/patient-not-by-contract/${each.value.name_suffix}"
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
    unit       = "Milliseconds"
  }
}

# Latency for Patient endpoints by contract with count = 4000 (for SLOs) with partner client SSL as
# dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_patient_by_contract_count_4000" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/patient-by-contract-count-4000/${each.value.name_suffix}"
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
    unit       = "Milliseconds"
  }
}

# Latency per-KB for all EoB endpoints (for SLOs) with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_by_kb_eob_all" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency-by-kb/eob-all/${each.value.name_suffix}"
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
    unit       = "Milliseconds"
  }
}

# Latency for EoB endpoints _with_ resources returned with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_eob_all_with_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/eob-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.eob_all}\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.resources_returned_count != 0 && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/eob-all-with-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Latency by KB for all EoB endpoints _with_ resources returned with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_by_kb_eob_all_with_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency-by-kb/eob-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.eob_all}\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.resources_returned_count != 0 && ",
    "$.mdc.http_access_response_duration_per_kb = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency-by-kb/eob-all-with-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_per_kb"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Latency for all EoB endpoints with no resources returned (for SLOs) with partner client SSL as
# dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_eob_all_no_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/eob-all-no-resources/${each.value.name_suffix}"
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
    unit       = "Milliseconds"
  }
}

# Latency by KB for all Claim endpoints _with_ resources returned with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_by_kb_claim_all_with_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency-by-kb/claim-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.claim_all}\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.resources_returned_count != 0 && ",
    "$.mdc.http_access_response_duration_per_kb = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency-by-kb/claim-all-with-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_per_kb"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Latency for all Claim endpoints with no resources returned (for SLOs) with partner client SSL as
# dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_claim_all_no_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/claim-all-no-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.claim_all}\" && ",
    "$.mdc.resources_returned_count = 0 && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/claim-all-no-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Latency for Claim endpoints _with_ resources returned with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_claim_all_with_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/claim-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.claim_all}\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.resources_returned_count != 0 && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/claim-all-with-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Latency by KB for all ClaimResponse endpoints _with_ resources returned with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_by_kb_claimresponse_all_with_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency-by-kb/claimresponse-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.claim_response_all}\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.resources_returned_count != 0 && ",
    "$.mdc.http_access_response_duration_per_kb = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency-by-kb/claimresponse-all-with-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_per_kb"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Latency for all ClaimResponse endpoints with no resources returned (for SLOs) with partner client SSL as
# dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_claimresponse_all_no_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/claimresponse-all-no-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.claim_response_all}\" && ",
    "$.mdc.resources_returned_count = 0 && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/claimresponse-all-no-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Latency for ClaimResponse endpoints _with_ resources returned with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency_claimresponse_all_with_resources" {
  for_each = local.filter_variations

  name           = "bfd-${local.env}/bfd-server/http-requests/latency/claimresponse-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{$.mdc.http_access_request_uri = \"${local.endpoints.claim_response_all}\" && ",
    "${local.client_ssl_pattern} && ",
    "$.mdc.resources_returned_count != 0 && ",
    "$.mdc.http_access_response_duration_milliseconds = *}"
  ])

  metric_transformation {
    name       = "http-requests/latency/claimresponse-all-with-resources"
    namespace  = local.namespace
    value      = "$.mdc.http_access_response_duration_milliseconds"
    dimensions = each.value.dimensions
    unit       = "Milliseconds"
  }
}

# Count of WARNING messages from the QueryLoggingListener indicates an unknown query type
resource "aws_cloudwatch_log_metric_filter" "query_logging_listener_count_warning_messages" {
  name           = "bfd-${local.env}/bfd-server/query-logging-listener/count/warning"
  log_group_name = local.log_groups.messages
  pattern        = "{$.logger = \"gov.cms.bfd.server.war.QueryLoggingListener\"}"

  metric_transformation {
    name      = "query-logging-listener/count/warning"
    namespace = local.namespace
    value     = "1"
    unit      = "Count"
  }
}
