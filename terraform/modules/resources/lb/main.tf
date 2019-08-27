# ELBv2 
# 
# Create an internal application LB with TCP listeners. 
#
locals {
  tags        = merge({Layer=var.layer, role=var.role}, var.env_config.tags)
}

# Subnets
# 
# Subnets are created by CCS VPC setup
#
data "aws_subnet" "app_subnets" {
  count             = length(var.env_config.azs)
  vpc_id            = var.env_config.vpc_id
  availability_zone = var.env_config.azs[count.index]
  filter {
    name    = "tag:Layer"
    values  = [var.layer] 
  }
}

# LB 
#
# Use NLB to allow the EC2 instance to terminate the TLS connection 
#
resource "aws_lb" "main" {
  name                = "bfd-${var.env_config.env}-${var.role}"
  tags                = local.tags
  load_balancer_type  = var.load_balancer_type
  internal            = true
  subnets             = data.aws_subnet.app_subnets[*].id

  enable_cross_zone_load_balancing = true
}

# Listener
#
resource "aws_lb_listener" "main" {
  load_balancer_arn = aws_lb.main.arn
  port              = var.ingress_port
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.main.arn
  }
}

# Target group
#
resource "aws_lb_target_group" "main" {
  name              = "bfd-${var.env_config.env}-${var.role}"
  target_type       = "instance"
  protocol          = "TCP"
  port              = var.egress_port
  vpc_id            = var.env_config.vpc_id
  tags              = local.tags

  health_check {
    enabled         = true
    protocol        = "TCP"
    port            = "traffic-port"

    # Comes from GDIT environment
    interval            = 10 
    healthy_threshold   = 5
    unhealthy_threshold = 5
  }
}

