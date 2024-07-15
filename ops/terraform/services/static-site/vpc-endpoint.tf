resource "aws_security_group" "allow_vpce" {
  name        = "allowVPCEndpoint"
  description = "Allow VPC Endpoint traffic"
  vpc_id      = data.aws_vpc.this.id
}

resource "aws_vpc_security_group_ingress_rule" "allow_http_ipv4" {
  security_group_id = aws_security_group.allow_vpce.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "allow_tls_ipv4" {
  security_group_id = aws_security_group.allow_vpce.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.allow_vpce.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id       = data.aws_vpc.this.id
  service_name = "com.amazonaws.${local.region}.s3"

  vpc_endpoint_type   = "Interface"
  private_dns_enabled = true

  subnet_ids         = data.aws_subnets.env_subnets.ids
  security_group_ids = [aws_security_group.allow_vpce.id]

  dns_options {
    dns_record_ip_type                             = "ipv4"
    private_dns_only_for_inbound_resolver_endpoint = false
  }

  policy = jsonencode({
    "Statement" : [
      {
        "Action" : "s3:GetObject",
        "Effect" : "Allow",
        "Principal" : "*",
        "Resource" : [
          aws_s3_bucket.static_site.arn,
          "${aws_s3_bucket.static_site.arn}/*"
        ]
      }
    ]
  })
}