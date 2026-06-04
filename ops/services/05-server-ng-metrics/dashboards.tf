locals {
  dashboard_name_prefix = "bfd-${local.env}-${local.target_service}"
}

resource "aws_cloudwatch_dashboard" "bfd_dashboard" {
  dashboard_name = local.dashboard_name_prefix
  dashboard_body = templatefile(
    "${path.module}/templates/bfd-server-ng-dashboard.json.tftpl",
    {
      namespace = local.namespace
      env       = local.env
      region    = local.region
      service   = local.target_service
    }
  )
}