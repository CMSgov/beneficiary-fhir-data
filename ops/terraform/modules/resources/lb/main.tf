# LB
# 
# Create an internal application LB with TCP listeners. 
#
locals {
  tags        = merge({Layer=var.layer, role=var.role}, var.env_config.tags)
  log_prefix  = "${var.role}_elb_access_logs" 
}

##
# Find context
##

# Accounts 
#
data "aws_caller_identity" "current" {}
data "aws_elb_service_account" "main" {}

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

# S3 bucket for logs
#
data "aws_s3_bucket" "logs" {
  bucket = var.log_bucket
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

  internal              = !var.is_public
  subnets               = data.aws_subnet.app_subnets[*].id # Gives AZs and VPC association
  security_groups       = [aws_security_group.lb.id]

  cross_zone_load_balancing   = false   # Match HealthApt
  idle_timeout                = 60      # (seconds) Match HealthApt
  connection_draining         = true
  connection_draining_timeout = 60

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
    enabled             = false
    bucket              = var.log_bucket
    bucket_prefix       = local.log_prefix
    interval            = 5   # (minutes) Match HealthApt      
  }
}

# Security Group for LB
#
resource "aws_security_group" "lb" {
  name            = "bfd-${var.env_config.env}-${var.role}-lb"
  description     = "Allow access to the ${var.role} load-balancer"
  vpc_id          = var.env_config.vpc_id
  tags            = merge({Name="bfd-${var.env_config.env}-${var.role}-lb"}, local.tags)

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

# Policy for S3 log access
# 
resource "aws_s3_bucket_policy" "logs" {
  bucket = data.aws_s3_bucket.logs.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "LBAccessLogs",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
          "AWS": ["${data.aws_elb_service_account.main.arn}"]
      },
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::${var.log_bucket}/*"
    }
  ]
}
POLICY
}