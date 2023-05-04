data "aws_caller_identity" "current" {}

data "aws_ssm_parameters_by_path" "sensitive_service" {
  path            = "/bfd/${local.env}/${local.service}/sensitive"
  with_decryption = true
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

data "aws_vpc" "this" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
  }
}

data "aws_subnet" "this" {
  for_each = local.subnet_ip_reservations

  vpc_id = local.vpc_id
  filter {
    name   = "tag:Name"
    values = [each.key]
  }
}
