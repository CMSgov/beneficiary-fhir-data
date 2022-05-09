resource "aws_cloudwatch_dashboard" "bfd-dashboards" {
  dashboard_name = var.dashboard_name
  dashboard_body = templatefile("../../../../../modules/resources/bfd_cw_dashboards/templates/bfd-server-logs.json", {
    dashboard_namespace = var.dashboard_namespace
  })
}