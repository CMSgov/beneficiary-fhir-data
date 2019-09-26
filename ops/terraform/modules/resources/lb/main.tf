# LB
# 
# Create an internal application LB with TCP listeners. 
#
locals {
  tags        = merge({Layer=var.layer, role=var.role}, var.env_config.tags)
}

##
# Find context
##

# Account 
#
data "aws_caller_identity" "current" {}

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

##
# Create resources
##

# LB 
#
# A ELB v1 TCP allows the EC2 instance to terminate the TLS connection, matching the HealthApt environment. 
#
resource "aws_elb" "main" {
  name                  = "bfd-${var.env_config.env}-${var.role}"
  tags                  = local.tags

  internal              = true
  subnets               = data.aws_subnet.app_subnets[*].id # Gives AZs and VPC association
  security_groups       = [aws_security_group.lb.id]

  cross_zone_load_balancing = false   # Match HealthApt
  idle_timeout              = 60      # (seconds) Match HealthApt
  connection_draining       = false   # Match HealthApt

  listener {
    lb_protocol         = "TCP"
    lb_port             = var.ingress.port
    instance_protocol   = "TCP"
    instance_port       = var.egress.port
  }

  health_check {
    healthy_threshold   = 5   # Match HealthApt
    unhealthy_threshold = 2   # Match HealthApt
    target              = "TCP:${var.egress.port}"
    interval            = 10  # (seconds) Match HealthApt
    timeout             = 5   # (seconds) Match HealthApt
  } 

  access_logs {
    enabled             = true
    bucket              = var.log_bucket
    bucket_prefix       = "elb_access_logs/pd"
    interval            = 5   # (minutes) Match HealthApt      
  }
}

# Security Group for LB
#
resource "aws_security_group" "lb" {
  name            = "bfd-${var.env_config.env}-${var.role}-lb"
  description     = "Allow access to the ${var.role} load-balancer"
  vpc_id          = var.env_config.vpc_id
  tags            = merge({Name="bfd-${var.env_config.env}-${var.role}-base"}, local.tags)

  ingress {
    from_port     = var.ingress.port
    to_port       = var.ingress.port
    protocol      = "tcp"
    cidr_blocks   = var.ingress.cidr_blocks
    description   = var.ingress.description
  }

  egress {
    from_port     = var.egress.port
    to_port       = var.egress.port
    protocol      = "tcp"
    cidr_blocks   = var.egress.cidr_blocks
    description   = var.egress.description
  }
}
