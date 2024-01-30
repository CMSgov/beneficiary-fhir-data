locals {
  vpc_id = var.env_config.vpc_id
  env    = var.env_config.env
}

# Create a private zone for the environment
resource "aws_route53_zone" "main" {
  name          = "bfd-${local.env}.local"
  comment       = "BFD private zone for ${local.env}."
  force_destroy = true

  # VPC is only valid for private zones
  dynamic "vpc" {
    for_each = ["dummy"]
    content {
      vpc_id = local.vpc_id
    }
  }
}

