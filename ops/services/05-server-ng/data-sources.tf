data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

data "aws_ssm_parameter" "zone_name" {
  name            = "/bfd/platform/network/sensitive/route53/zone/${local.parent_env}/domain"
  with_decryption = true
}

data "aws_route53_zone" "parent_env" {
  name         = nonsensitive(data.aws_ssm_parameter.zone_name.value)
  private_zone = true
  tags = {
    "ConfigId" = local.parent_env
  }
}

data "aws_ram_resource_share" "pace_ca" {
  resource_owner = "OTHER-ACCOUNTS"
  name           = "pace-ca-g1"
}

data "aws_acmpca_certificate_authority" "pace" {
  arn = one(data.aws_ram_resource_share.pace_ca.resource_arns)
}
