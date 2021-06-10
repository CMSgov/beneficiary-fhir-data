# Creates the BFD Server CloudWatch log metric filters and log group. 
#

locals {

  log_groups = {
    access = "/bfd/${var.env}/bfd-server/access.txt"
  }
}

resource "aws_cloudwatch_log_metric_filter" "mct_query_time" {
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/mct"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user=*mct*, timestamp, request, query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/mct"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "$duration_milliseconds"
    default_value = null
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request, query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-coverage" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/coverage"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/Coverage*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/coverage"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-eob" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/eob"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/ExplanationOfBenefit*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/eob"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-500" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/http-500"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request, query_string, status_code = 500, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/http-500"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-metadata" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/metadata"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/metadata*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/metadata"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-not-2xx" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/not-2xx"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request, query_string, status_code != 2*, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/not-2xx"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-patient" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/patient"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/Patient*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/patient"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-patient-patientSearchById" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/patient/patientSearchById"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/Patient*_id=*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/patient/patientSearchById"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-count-patient-patientSearchByIdentifier" {
  name           = "bfd-${var.env}/bfd-server/http-requests/count/patient/patientSearchByIdentifier"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/Patient*identifier=*hicnHash*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/count/patient/patientSearchByIdentifier"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-latency" {
  name           = "bfd-${var.env}/bfd-server/http-requests/latency"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request, query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "$duration_milliseconds"
    default_value = null
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-latency-fhir-coverage" {
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/fhir/coverage"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/Coverage*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/fhir/coverage"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "$duration_milliseconds"
    default_value = null
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-latency-fhir-eob" {
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/fhir/eob"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/ExplanationOfBenefit*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/fhir/eob"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "$duration_milliseconds"
    default_value = null
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-latency-fhir-patient" {
  name           = "bfd-${var.env}/bfd-server/http-requests/latency/fhir/patient"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request = \"*/Patient*\", query_string, status_code, bytes, duration_milliseconds, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/fhir/patient"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "$duration_milliseconds"
    default_value = null
  }
}

resource "aws_cloudwatch_log_metric_filter" "http-requests-latency-over-6000" {
  count = var.env == null ? 0 : 1

  name           = "bfd-${var.env}/bfd-server/http-requests/latency/over-6000"
  pattern        = "[remote_host_name, remote_logical_username, remote_authenticated_user, timestamp, request, query_string, status_code, bytes, duration_milliseconds > 6000, original_query_id, original_query_counter, original_query_timestamp, developer, developer_name, application_id, application, user_id, user, beneficiary_id]"
  log_group_name = local.log_groups.access

  metric_transformation {
    name          = "http-requests/latency/over-6000"
    namespace     = "bfd-${var.env}/bfd-server"
    value         = "1"
    default_value = "0"
  }
}

