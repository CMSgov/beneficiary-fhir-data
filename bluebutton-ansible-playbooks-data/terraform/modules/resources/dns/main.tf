resource "aws_route53_zone" "main" {
  vpc_id  = "${var.vpc_id}"
  name    = "bfd-${var.env}.local"
  comment = "BFD private hosted zone (${var.env})"

  tags {
    Environment = "${var.env}"
  }
}
