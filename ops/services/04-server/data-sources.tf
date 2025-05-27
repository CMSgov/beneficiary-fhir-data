data "aws_vpc" "mgmt" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_vpc_peering_connection" "peers" {
  for_each = nonsensitive(toset(jsondecode(local.ssm_config["/bfd/${local.service}/lb_vpc_peerings_json"])))

  tags = {
    Name = each.key
  }
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

data "aws_ec2_managed_prefix_list" "jenkins" {
  filter {
    name   = "prefix-list-name"
    values = ["bfd-cbc-jenkins"]
  }
}

data "aws_ssm_parameter" "zone_name" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain"
  with_decryption = true
}

data "aws_ssm_parameter" "zone_is_private" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_is_private"
  with_decryption = true
}

data "aws_route53_zone" "root" {
  name         = nonsensitive(data.aws_ssm_parameter.zone_name.value)
  private_zone = nonsensitive(data.aws_ssm_parameter.zone_is_private.value)
  tags = {
    "ConfigId" = "root"
  }
}
