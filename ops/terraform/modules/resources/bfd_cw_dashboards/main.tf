# This module is for defining the BDF CloudWatch dashboard
#
locals {
  app = "bfd-server"

  namespace = "bfd-${var.env}/${local.app}"
  # This metric is not used -- instead, it is used to collect the proper client_ssls per-environment
  all_requests_count_metric = "http-requests/count/all"

  partner_client_ssl_regexs = {
    internal_client_ssl = {
      prod_sbx = ".*Internal.*"
      prod     = ".*Internal.*"
      test     = ".*Internal.*"
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
    for key, client_ssl in data.external.client_ssls.result :
    key => coalesce(client_ssl, "NOT_APPLICABLE")
  }
}

data "template_file" "dashboard-template" {
  template = "${file("${path.module}/templates/bfd-dashboards.tpl")}"
  vars     = merge({ namespace = local.namespace }, local.client_ssls)
}

resource "aws_cloudwatch_dashboard" "bfd-dashboards-fhir" {
  dashboard_name = var.dashboard_name
  dashboard_body = data.template_file.dashboard-template.rendered
}
