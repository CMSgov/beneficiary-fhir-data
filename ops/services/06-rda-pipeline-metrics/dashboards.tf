locals {
  dashboards_name_prefix = "bfd-${local.env}-${local.target_service}"
}

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = local.dashboards_name_prefix
  dashboard_body = templatefile(
    "${path.module}/templates/rda_pipeline_dashboard.json.tftpl",
    {
      env       = local.env
      region    = local.region
      namespace = local.metrics_namespace
    }
  )
}
