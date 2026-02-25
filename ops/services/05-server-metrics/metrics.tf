locals {
  log_groups = {
    access   = data.aws_cloudwatch_log_group.access.name
    messages = data.aws_cloudwatch_log_group.messages.name
  }

  endpoints = {
    all                = { match = "*/fhir/*" }
    metadata           = { match = "*/fhir/metadata*" }
    coverage_all       = { match = "*/fhir/Coverage*" }
    patient_all        = { match = "*/fhir/Patient*" }
    eob_all            = { match = "*/fhir/ExplanationOfBenefit*" }
    claim_all          = { match = "*/fhir/Claim*", no_match = "*/fhir/ClaimResponse*" }
    claim_response_all = { match = "*/fhir/ClaimResponse*" }
  }
  endpoint_patterns = {
    for name, patterns in local.endpoints :
    name => "(${join(" && ", [
      "$.mdc.http_access_request_uri = \"${patterns.match}\"",
      "$.mdc.http_access_request_uri != \"${try(patterns.no_match, "")}\"",
    ])})"
  }
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
      "${endpoint_key}_${variation}" => {
        "resource_name"    = replace(endpoint_key, "_", "-")
        "endpoint_pattern" = endpoint_pattern
        "name_suffix"      = variation_config.name_suffix
        "dimensions"       = variation_config.dimensions
      }
    }
  ]...)

  client_ssl_pattern = "$.mdc.http_access_request_clientSSL_DN = \"*\""
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_cloudwatch_log_group" "access" {
  name = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.target_service}/${local.target_service}/access"
}

data "aws_cloudwatch_log_group" "messages" {
  name = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.target_service}/${local.target_service}/messages"
}

# Count requests per endpoint with partner client SSL as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count" {
  for_each = local.endpoint_filters_config

  name           = "${local.namespace}/http-requests/count/${each.value.resource_name}/${each.value.name_suffix}"
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

  name           = "${local.namespace}/http-requests/latency/${each.value.resource_name}/${each.value.name_suffix}"
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

  name           = "${local.namespace}/http-requests/count/500-responses/${each.value.name_suffix}"
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

  name           = "${local.namespace}/http-requests/count/non-2xx-responses/${each.value.name_suffix}"
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

  name           = "${local.namespace}/http-requests/latency/patient-not-by-contract/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  # Terraform HCL has no support for breaking long strings, so the join() function is used as a
  # poor, but functional, substitute. Otherwise this pattern would be far too long
  pattern = join("", [
    "{${local.endpoint_patterns.patient_all} && ",
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

  name           = "${local.namespace}/http-requests/latency/patient-by-contract-count-4000/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.patient_all} && ",
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

  name           = "${local.namespace}/http-requests/latency-by-kb/eob-all/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.eob_all} && ",
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

  name           = "${local.namespace}/http-requests/latency/eob-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.eob_all} && ",
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

  name           = "${local.namespace}/http-requests/latency-by-kb/eob-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.eob_all} && ",
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

  name           = "${local.namespace}/http-requests/latency/eob-all-no-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.eob_all} && ",
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

  name           = "${local.namespace}/http-requests/latency-by-kb/claim-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.claim_all} && ",
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

  name           = "${local.namespace}/http-requests/latency/claim-all-no-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.claim_all} && ",
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

  name           = "${local.namespace}/http-requests/latency/claim-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.claim_all} && ",
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

  name           = "${local.namespace}/http-requests/latency-by-kb/claimresponse-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.claim_response_all} && ",
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

  name           = "${local.namespace}/http-requests/latency/claimresponse-all-no-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.claim_response_all} && ",
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

  name           = "${local.namespace}/http-requests/latency/claimresponse-all-with-resources/${each.value.name_suffix}"
  log_group_name = local.log_groups.access

  pattern = join("", [
    "{${local.endpoint_patterns.claim_response_all} && ",
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
  name           = "${local.namespace}/query-logging-listener/count/warning"
  log_group_name = local.log_groups.messages
  pattern        = "{$.logger = \"gov.cms.bfd.server.war.QueryLoggingListener\"}"

  metric_transformation {
    name      = "query-logging-listener/count/warning"
    namespace = local.namespace
    value     = "1"
    unit      = "Count"
  }
}

resource "aws_cloudwatch_log_metric_filter" "samhsa_mismatch_error_count" {
  name           = "${local.namespace}/samhsa-mismatch/count/error"
  log_group_name = local.log_groups.messages
  pattern        = "{ $.message = %Samhsa: Claim ID mismatch between old SAMHSA filter% }"

  metric_transformation {
    name      = "samhsa-mismatch/count/error"
    namespace = local.namespace
    value     = "1"
    unit      = "Count"
  }
}
