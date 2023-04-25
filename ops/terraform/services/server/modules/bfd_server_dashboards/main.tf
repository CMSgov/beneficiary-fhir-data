# This module is for defining the BDF CloudWatch dashboard
#
locals {
  env = terraform.workspace
  app = "bfd-server"

  namespace = "bfd-${local.env}/${local.app}"
  # This metric is not used -- instead, it is used to collect the proper client_ssls per-environment
  all_requests_count_metric = "http-requests/count/all"

  partner_client_ssl_regexs = {
    internal_client_ssl = {
      prod_sbx = ".*bluebutton-backend.*test.*"
      prod     = ".*bluebutton-backend.*test.*"
      test     = ".*bluebutton-backend.*test.*"
    }
    ab2d_client_ssl = {
      prod_sbx = ".*ab2d-sbx-client.*"
      prod     = ".*ab2d-prod-client.*"
    }
    bcda_client_ssl = {
      prod_sbx = ".*bcda-sbx-client.*"
      prod     = ".*bcda-prod-client.*"
    }
    dpc_client_ssl = {
      prod_sbx = ".*dpc-prod-sbx-client.*"
      # jq requires escaped characters be escaped with 2 backslashes
      prod = ".*dpc\\\\.prod\\\\.client.*"
    }
    bb_client_ssl = {
      prod_sbx = ".*BlueButton.*"
      prod     = ".*BlueButton.*"
    }
  }

  client_ssls = {
    for key in keys(local.partner_client_ssl_regexs) :
    key => coalesce(lookup(data.external.client_ssls.result, key, null), "NONE")
  }
}

resource "aws_cloudwatch_dashboard" "bfd_dashboard" {
  dashboard_name = var.dashboard_name
  dashboard_body = templatefile(
    "${path.module}/templates/bfd-dashboard.tftpl",
    merge({
      namespace = local.namespace
      env       = local.env
    }, local.client_ssls)
  )
}

resource "aws_cloudwatch_dashboard" "bfd_dashboard_slos" {
  dashboard_name = "${var.dashboard_name}-slos"
  dashboard_body = templatefile(
    "${path.module}/templates/bfd-dashboard-slos.tftpl",
    merge({
      namespace = local.namespace
      env       = local.env
    }, local.client_ssls)
  )
}
