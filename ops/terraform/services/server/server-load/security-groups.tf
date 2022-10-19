resource "aws_security_group" "this" {
  description = "Allow ${local.service} intra-group communications in ${local.env}"
  name        = "bfd-${local.env}-${local.service}"
  vpc_id      = data.aws_vpc.main.id

  egress {
    description = "Permissive egress"
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Locust Controller Service Port 5557"
    from_port   = 5557
    protocol    = "tcp"
    self        = true
    to_port     = 5557
  }
}

resource "aws_security_group_rule" "rds" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  description              = "Allow ${local.service} access in ${local.env}"
  security_group_id        = data.aws_security_group.rds.id
  source_security_group_id = aws_security_group.this.id
}
