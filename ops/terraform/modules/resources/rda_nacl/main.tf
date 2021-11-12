# NACL to manage communication between the BFD and RDA environments.
# The NACL rules will open communication over the known ports and close
# everything else.  This is done by creating a "lower" rule that explicitly
# allows communication and defining a "upper" rule that defaults to denying
resource "aws_network_acl" "rda" {
  # only create the NACL if the CIDR block has been defined
  count  = var.mpm_rda_cidr_block != null ? 1 : 0
  vpc_id = var.env_config.vpc_id
  tags   = var.env_config.tags
}

# When enabled, allow communication over 443 otherwise shutoff all communication
resource "aws_network_acl_rule" "rda" {
  count = var.mpm_rda_cidr_block != null ? 1 : 0

  network_acl_id = aws_network_acl.rda[0].id
  rule_number    = 100
  protocol       = var.mpm_enabled ? "tcp" : -1
  rule_action    = var.mpm_enabled ? "allow" : "deny"
  cidr_block     = var.mpm_rda_cidr_block
  from_port      = var.mpm_enabled ? 443 : 0
  to_port        = var.mpm_enabled ? 443 : 0
}
