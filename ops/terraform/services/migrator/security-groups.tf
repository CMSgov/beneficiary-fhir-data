# TODO: consider refactoring, hoisting security group and group rules into a centrally managed environmental configuration for services

resource "aws_security_group" "this" {
  count       = local.migrator_instance_count
  description = "${local.service} application security group in ${local.env}"
  name        = "bfd-${local.env}-${local.service}-app"
  tags        = { Name = "bfd-${local.env}-${local.service}-app" }
  vpc_id      = data.aws_vpc.main.id

  # NOTE: This application does not currently listen on any ports, so no ingress rules are needed.

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group_rule" "rds" {
  count                    = local.migrator_instance_count
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  description              = "Allow ${local.service} access in ${local.env}"
  security_group_id        = data.aws_security_group.rds.id
  source_security_group_id = aws_security_group.this[0].id
}
