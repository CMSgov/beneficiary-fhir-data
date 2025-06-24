data "aws_vpc" "mgmt" {
  count = !var.greenfield ? 1 : 0

  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_ec2_managed_prefix_list" "jenkins" {
  count = !var.greenfield ? 1 : 0

  filter {
    name   = "prefix-list-name"
    values = ["bfd-cbc-jenkins"]
  }
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

data "aws_ssm_parameter" "zones_list" {
  name = !var.greenfield ? "/bfd/mgmt/common/nonsensitive/r53_hosted_zones_json" : "/bfd/platform/network/nonsensitive/route53/zones_list_json"
}

data "aws_ssm_parameter" "zone_name" {
  count = local.root_zone_configured ? 1 : 0

  name            = !var.greenfield ? "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain" : "/bfd/platform/network/sensitive/route53/zone/root/domain"
  with_decryption = true
}

data "aws_route53_zone" "root" {
  count = local.root_zone_configured ? 1 : 0

  name         = nonsensitive(one(data.aws_ssm_parameter.zone_name[*].value))
  private_zone = true
  tags = {
    "ConfigId" = "root"
  }
}
