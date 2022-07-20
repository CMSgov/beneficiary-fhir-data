resource "aws_security_group" "this" {
  description = "${local.service} lambda security group in ${local.env}"
  name        = "bfd-${local.env}-${local.service}-lambda"
  tags        = merge(local.common_tags, { Name = "bfd-${local.env}-${local.service}-lambda" })
  vpc_id      = data.aws_vpc.main.id

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}