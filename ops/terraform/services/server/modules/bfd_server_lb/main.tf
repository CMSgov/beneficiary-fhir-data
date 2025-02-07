# Create an internal application LB with TCP listeners. 
#

locals {
  env             = terraform.workspace
  additional_tags = { Layer = var.layer, role = var.role }
  log_prefix      = "${var.role}_elb_access_logs"
}

## RESOURCES
#

# classic ELB 
resource "aws_elb" "main" {
  name = "bfd-${local.env}-${var.role}"
  tags = local.additional_tags

  internal        = !var.is_public
  subnets         = data.aws_subnet.app_subnets[*].id # Gives AZs and VPC association
  security_groups = [aws_security_group.lb.id]

  cross_zone_load_balancing   = false # Match HealthApt
  idle_timeout                = 60    # (seconds) Match HealthApt
  connection_draining         = true
  connection_draining_timeout = 60

  listener {
    lb_protocol       = "TCP"
    lb_port           = var.ingress.port
    instance_protocol = "TCP"
    instance_port     = var.egress.port
  }

  health_check {
    healthy_threshold   = 3 
    unhealthy_threshold = 5 # Server, on avg, inits within 40 seconds; this gives enough time for it to do so
    target              = "TCP:${var.egress.port}"
    interval            = 10 # (seconds) Match HealthApt
    timeout             = 5  # (seconds) Match HealthApt
  }
}


