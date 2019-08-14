resource "aws_elb" "default" {
  name = "${var.name}"

  availability_zones = [
    "us-east-1a",
    "us-east-1b",
    "us-east-1c",
  ]

  access_logs {
    bucket   = "bfd-${var.env}-logs-lb-access"
    interval = "${var.access_log_interval}"
  }

  listener {
    instance_port     = 80
    instance_protocol = "http"
    lb_port           = 80
    lb_protocol       = "http"
  }

  listener {
    instance_port      = 443
    instance_protocol  = "https"
    lb_port            = 443
    lb_protocol        = "https"
    ssl_certificate_id = "${var.ssl_certificate_id}"
  }

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 5
    timeout             = 5
    target              = "HTTPS:443/health"
    interval            = 10
  }

  cross_zone_load_balancing   = true
  idle_timeout                = "${var.idle_timeout}"
  connection_draining         = true
  connection_draining_timeout = 300

  tags {
    Name        = "bfd-${var.env}-clb-1"
    Function    = "ClassicLoadBalancer"
    Environment = "${upper(var.env)}"
    Application = "bfs"
    Business    = "OEDA"
  }
}
