resource "aws_route53_zone" "zone" {
  comment = "Managed by Terraform"
  name    = "bfd-mgmt.local"
  tags    = local.shared_tags

  vpc {
    vpc_id     = data.aws_vpc.main.id
    vpc_region = "us-east-1"
  }
}
