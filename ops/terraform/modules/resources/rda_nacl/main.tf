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

# When `mpm_enabled` is set to true, the known block will have a rule number
# lower than the default to allow communication over the accepted port. Otherwise
# the rule number will be higher than the default, which will cause ALL
# communication to stop
resource "aws_network_acl_rule" "rda_known" {
  count = var.mpm_rda_cidr_block != null ? 1 : 0

  network_acl_id = aws_network_acl.rda[0].id
  rule_number    = var.mpm_enabled ? 100 : 120
  protocol       = "tcp"
  rule_action    = "allow"
  cidr_block     = var.mpm_rda_cidr_block
  from_port      = 443
  to_port        = 443
}

resource "aws_network_acl_rule" "rda_default" {
  count = var.mpm_rda_cidr_block != null ? 1 : 0

  network_acl_id = aws_network_acl.rda[0].id
  rule_number    = 110
  protocol       = -1
  rule_action    = "deny"
  cidr_block     = var.mpm_rda_cidr_block
  from_port      = 0
  to_port        = 0
}
