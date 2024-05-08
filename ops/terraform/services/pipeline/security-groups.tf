resource "aws_security_group" "app" {
  description = "Access specific to the BFD Pipeline application."
  egress = [
    {
      cidr_blocks = [
        "0.0.0.0/0",
      ]
      description      = ""
      from_port        = 0
      ipv6_cidr_blocks = []
      prefix_list_ids  = []
      protocol         = "-1"
      security_groups  = []
      self             = false
      to_port          = 0
    },
  ]
  name                   = "bfd-${local.env}-${local.legacy_service}-app"
  revoke_rules_on_delete = false
  tags                   = { Name = "bfd-${local.env}-${local.legacy_service}-app" }
  vpc_id                 = local.vpc_id
}

resource "aws_security_group_rule" "allow_db_primary_access" {
  for_each                 = toset(local.rds_security_group_ids)
  description              = "Allows BFD Pipeline access to the primary DB."
  from_port                = 5432
  protocol                 = "tcp"
  security_group_id        = each.value
  source_security_group_id = aws_security_group.app.id
  to_port                  = 5432
  type                     = "ingress"
}
