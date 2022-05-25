# This module is for defining the BDF CloudWatch dashboard
#

data "template_file" "dashboard-template" {
  template = "${file("${path.module}/templates/bfd-dashboards.tpl")}"
  vars = {
    asg_name            = var.asg
    dashboard_namespace = var.dashboard_namespace
  }
}

resource "aws_cloudwatch_dashboard" "bfd-dashboards-fhir" {
  dashboard_name = var.dashboard_name
  dashboard_body = data.template_file.dashboard-template.rendered
}
