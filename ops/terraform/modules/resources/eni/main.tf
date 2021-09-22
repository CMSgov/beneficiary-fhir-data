locals {
  tags = merge({ Name = var.name }, var.env_config.tags)
}

resource "aws_network_interface" "main" {
  subnet_id         = var.subnet_id
  description       = var.description
  private_ips       = var.private_ips
  source_dest_check = var.source_dest_check
  tags              = local.tags
}
