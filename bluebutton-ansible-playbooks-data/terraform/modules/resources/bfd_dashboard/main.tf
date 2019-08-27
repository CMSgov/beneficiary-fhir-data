##
#
# NOTE: This module is for defining a main CloudWatch dashboard
#
##

data "template_file" "dashboard-template" {
  template = "${file("${path.module}/dashboard-template.tpl")}"

  vars {
    app                = "${var.app}"
    env                = "${var.env}"
    vpc_name           = "${var.vpc_name}"
    load_balancer_name = "${var.load_balancer_name}"
    asg_name           = "${var.asg_name}"
    rds_name           = "${var.rds_name}"
    nat_gw_name        = "${var.nat_gw_name}"
  }
}

resource "aws_cloudwatch_dashboard" "main-dash" {
  count = "${var.dashboard_enable}"

  dashboard_name = "MainDashboard-${var.app}-${var.env}"
  dashboard_body = "${data.template_file.dashboard-template.rendered}"
}
