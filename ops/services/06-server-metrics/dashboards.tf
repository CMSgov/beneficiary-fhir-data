locals {
  # NOTE: This will be largely unnecessary once BFD-3959 is complete as the MDC will include the
  # name of the partner, and so metrics will no longer need to work with "client SSLs" (certificate
  # DNs) to disambiguate a particular partner. This is why these variant values have not been
  # hoisted into SSM configuration
  partner_client_ssl_regexs = {
    internal_client_ssl = {
      sandbox  = ".*bluebutton-backend.*test.*"
      prod_sbx = ".*bluebutton-backend.*test.*"
      prod     = ".*bluebutton-backend.*test.*"
      test     = ".*bluebutton-backend.*test.*"
    }
    ab2d_client_ssl = {
      sandbox  = ".*ab2d-sbx-client.*"
      prod_sbx = ".*ab2d-sbx-client.*"
      prod     = ".*ab2d-prod-client.*"
    }
    ab2d_validation_client_ssl = {
      prod = ".*ab2d-prod-validation-client.*"
    }
    bcda_client_ssl = {
      sandbox  = ".*bcda-sbx-client.*"
      prod_sbx = ".*bcda-sbx-client.*"
      prod     = ".*bcda-prod-client.*"
    }
    dpc_client_ssl = {
      sandbox  = ".*dpc-prod-sbx-client.*"
      prod_sbx = ".*dpc-prod-sbx-client.*"
      # jq requires escaped characters be escaped with 2 backslashes
      prod = ".*dpc\\\\.prod\\\\.client.*"
    }
    bb_client_ssl = {
      sandbox  = ".*BlueButton Root CA.*"
      prod_sbx = ".*BlueButton Root CA.*"
      prod     = ".*BlueButton Root CA.*"
    }
  }

  client_ssls = {
    for key in keys(local.partner_client_ssl_regexs) :
    key => coalesce(lookup(data.external.client_ssls.result, key, null), "NONE")
  }

  dashboard_name_prefix = "bfd-${local.env}-${local.target_service}"
}

data "external" "client_ssls" {
  program = [
    "bash",
    "${path.module}/scripts/get-partner-client-ssl.sh",
    "http-requests/count/all",
    local.namespace,
    jsonencode({
      for partner, per_env_regex in local.partner_client_ssl_regexs :
      partner => lookup(per_env_regex, replace(local.env, "-", "_"), null)
    })
  ]
}

resource "aws_cloudwatch_dashboard" "bfd_dashboard" {
  dashboard_name = local.dashboard_name_prefix
  dashboard_body = templatefile(
    "${path.module}/templates/bfd-server-dashboard.json.tftpl",
    merge({
      namespace = local.namespace
      env       = local.env
      region    = local.region
      service   = local.target_service
    }, local.client_ssls)
  )
}

resource "aws_cloudwatch_dashboard" "bfd_dashboard_slos" {
  dashboard_name = "${local.dashboard_name_prefix}-slos"
  dashboard_body = templatefile(
    "${path.module}/templates/bfd-server-dashboard-slos.json.tftpl",
    merge({
      namespace = local.namespace
      env       = local.env
      region    = local.region
      service   = local.target_service
    }, local.client_ssls)
  )
}
