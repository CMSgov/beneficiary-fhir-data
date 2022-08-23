# This module is for defining the BDF CloudWatch dashboard
#

# BFD Server CloudWatch Dashboard
resource "aws_cloudwatch_dashboard" "bfd_server_dashboard" {
  dashboard_name = "bfd-server-${var.env_config.env}"
  dashboard_body = templatefile("${path.module}/templates/dashboard.tpl", { dashboard_namespace = "bfd-${var.env_config.env}/bfd-server" })
}
# BFD Server CloudWatch Dashboard
resource "aws_cloudwatch_dashboard" "bfd_server_aws_services_dashboard" {
  dashboard_name = "bfd-server-${var.env_config.env}"
  dashboard_body = templatefile("${path.module}/templates/aws-services-dashboard.tpl", { dashboard_namespace = "bfd-${var.env_config.env}/bfd-server" })
}
# BFD Server CloudWatch Dashboard
resource "aws_cloudwatch_dashboard" "bfd_server_opentelemetry_dashboard" {
  dashboard_name = "bfd-server-${var.env_config.env}"
  dashboard_body = templatefile("${path.module}/templates/opentelemetry-dashboard.tpl", { dashboard_namespace = "bfd-${var.env_config.env}/bfd-server" })
