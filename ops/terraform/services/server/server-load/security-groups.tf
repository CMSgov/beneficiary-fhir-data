resource "aws_security_group" "lambda" {
  description = "${local.service} lambda security group in ${local.env}"
  name        = "bfd-${local.env}-${local.service}-lambda"
  tags        = merge(local.shared_tags, { Name = "bfd-${local.env}-${local.service}-lambda" })
  vpc_id      = data.aws_vpc.main.id

  ingress {
    protocol    = "-1"
    self        = true # The security group itself will be added as a source to this ingress rule.
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    protocol    = "-1"
    self        = true # The security group itself will be added as a source to this egress rule.
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group_rule" "rds" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  description              = "Allow ${local.service} access in ${local.env}"
  security_group_id        = data.aws_security_group.rds.id
  source_security_group_id = aws_security_group.lambda.id
}
