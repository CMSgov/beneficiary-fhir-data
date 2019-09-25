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

# S3 buckets
#
# Bucket for access logs
#
data "aws_s3_bucket" "admin" {
  name  = "bfd-${var.env_config.env}-admin-${data.aws_caller_identity.current.account_id}"
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
  load_balancer_type    = "TCP"

  internal                          = true
  subnets                           = data.aws_subnet.app_subnets[*].id # Gives AZs and VPC association
  security_groups                   = var.security_groups

  enable_cross_zone_load_balancing  = false   # Match HealthApt
  idle_timeout                      = 60      # (seconds) Match HealthApt
  connection_draining               = false   # Match HealthApt

  listener {
    lb_protocol         = "TCP"
    lb_port             = var.ingress_port
    instance_protocol   = "TCP"
    instance_port       = var.egress_port
  }

  health_check {
    healthy_threshold   = 5   # Match HealthApt
    unhealthy_threshold = 2   # Match HealthApt
    target              = "TCP:${var.egress_port}"
    interval            = 10  # (seconds) Match HealthApt
    timeout             = 5   # (seconds) Match HealthApt
  } 

  access_logs {
    enabled             = true
    bucket              = data.aws_s3_bucket.admin.name
    bucket_prefix       = "elb_access_logs/pd"
    interval            = 5   # (minutes) Match HealthApt      
  }
}
