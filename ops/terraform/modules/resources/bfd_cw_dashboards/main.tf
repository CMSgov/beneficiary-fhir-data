# This module is for defining the BDF CloudWatch dashboard
#
# BFD Server CloudWatch Dashboard
resource "aws_cloudwatch_dashboard" "bfd_server_dashboard" {
  dashboard_name = "bfd-server-${var.env}"
  dashboard_body = templatefile("${path.module}/templates/dashboard.tpl", { dashboard_namespace = "bfd-${var.env}/bfd-server" })
}
# BFD Server CloudWatch Aws ServicesDashboard
resource "aws_cloudwatch_dashboard" "bfd_server_aws_services_dashboard" {
  dashboard_name = "bfd-server-${var.env}-aws-services"
  dashboard_body = templatefile("${path.module}/templates/aws-services-dashboard.tpl", { dashboard_namespace = "bfd-${var.env}/bfd-server", env = var.env })
}
# BFD Server CloudWatch OpenTelemetry Dashboard
resource "aws_cloudwatch_dashboard" "bfd_server_opentelemetry_dashboard" {
  dashboard_name = "bfd-server-${var.env}-opentelemetry"
  dashboard_body = templatefile("${path.module}/templates/opentelemetry-dashboard.tpl", { dashboard_namespace = "bfd-${var.env}/bfd-server" })
}