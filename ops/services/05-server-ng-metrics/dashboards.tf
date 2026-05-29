locals {
  partner_aliases = {
    internal_client_alias = {
      prod    = "bluebutton_backend_prod_data_server_client_test"
      sandbox = "bluebutton_backend_dpr_data_server_client_test"
      test    = "bluebutton_backend_test_data_server_client_test"
    }
    ab2d_client_alias = {
      prod    = "ab2d_cms_gov"
      sandbox = "ab2d_cms_gov"
      test    = "ab2d_cms_gov"
    }
    bcda_client_alias = {
      prod    = "bcda_prod_client"
      sandbox = "bcda_sbx_client"
      test    = "bcda_test_client"
    }
    dpc_client_alias = {
      sandbox = "dpc_prod_sbx_client"
    }
    bb_client_alias = {
      prod    = "bluebutton_root_ca"
      sandbox = "bluebutton_root_ca"
      test    = "bluebutton_root_ca"
    }
  }

  client_aliases = {
    for partner, aliases in local.partner_aliases :
    partner => coalesce(lookup(aliases, local.env, null), "NONE")
  }

  dashboard_name_prefix = "bfd-${local.env}-${local.target_service}"
}

resource "aws_cloudwatch_dashboard" "bfd_dashboard" {
  dashboard_name = local.dashboard_name_prefix
  dashboard_body = templatefile(
    "${path.module}/templates/bfd-server-ng-dashboard.json.tftpl",
    merge({
      namespace = local.namespace
      env       = local.env
      region    = local.region
      service   = local.target_service
      },
    local.client_aliases)
  )
}