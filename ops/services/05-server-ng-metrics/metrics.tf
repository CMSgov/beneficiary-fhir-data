locals {
  log_groups = {
    messages = data.aws_cloudwatch_log_group.messages.name
  }

  endpoints = {
    all          = { match = "*/fhir/*" }
    metadata     = { match = "*/fhir/metadata*" }
    coverage_all = { match = "*/fhir/Coverage*" }
    patient_all  = { match = "*/fhir/Patient*" }
    eob_all      = { match = "*/fhir/ExplanationOfBenefit*" }
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
        certificate_alias = "$.mdc.certificateAlias"
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

  log_type                  = "$.logType = \"requestTelemetry\""
  certificate_alias_pattern = "$.mdc.certificateAlias = \"*\""
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_cloudwatch_log_group" "messages" {
  name = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.target_service}/${local.target_service}/messages"
}

# Count requests per endpoint with partner certificate alias as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count" {
  for_each = local.endpoint_filters_config

  name           = "${local.namespace}/http-requests/count/${each.value.resource_name}/${each.value.name_suffix}"
  log_group_name = local.log_groups.messages

  pattern = join("", [
    "{${local.log_type} && ",
    "${each.value.endpoint_pattern} && ",
    "${local.certificate_alias_pattern}}",
  ])

  metric_transformation {
    name       = "http-requests/count/${each.value.resource_name}"
    namespace  = local.namespace
    value      = "1"
    dimensions = each.value.dimensions
    unit       = "Count"
  }
}

# Latency per endpoint with partner certificate alias as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_latency" {
  for_each = local.endpoint_filters_config

  name           = "${local.namespace}/http-requests/latency/${each.value.resource_name}/${each.value.name_suffix}"
  log_group_name = local.log_groups.messages

  pattern = join("", [
    "{${local.log_type} && ",
    "${each.value.endpoint_pattern} && ",
    "${local.certificate_alias_pattern} && ",
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

# Count HTTP 5xxs with partner certificate alias as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_500_responses" {
  for_each = local.filter_variations

  name           = "${local.namespace}/http-requests/count/500-responses/${each.value.name_suffix}"
  log_group_name = local.log_groups.messages

  pattern = join("", [
    "{${local.log_type} && ",
    "$.mdc.http_access_response_status = 500 && ",
    "${local.certificate_alias_pattern}}",
  ])
  metric_transformation {
    name       = "http-requests/count/500-responses"
    namespace  = local.namespace
    value      = "1"
    dimensions = each.value.dimensions
    unit       = "Count"
  }
}

# Count HTTP non-2XXs with partner certificate alias as dimension
resource "aws_cloudwatch_log_metric_filter" "http_requests_count_non_2xx_responses" {
  for_each = local.filter_variations

  name           = "${local.namespace}/http-requests/count/non-2xx-responses/${each.value.name_suffix}"
  log_group_name = local.log_groups.messages

  pattern = join("", [
    "{${local.log_type} && ",
    "$.mdc.http_access_response_status != 200 && ",
    "${local.certificate_alias_pattern}}",
  ])

  metric_transformation {
    name       = "http-requests/count/non-2xx-responses"
    namespace  = local.namespace
    value      = "1"
    dimensions = each.value.dimensions
    unit       = "Count"
  }
}
