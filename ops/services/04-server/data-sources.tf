data "aws_vpc" "mgmt" {
  count = !var.greenfield ? 1 : 0

  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_vpc_peering_connection" "peers" {
  for_each = !var.greenfield ? nonsensitive(toset(jsondecode(local.ssm_config["/bfd/${local.service}/lb_vpc_peerings_json"]))) : toset([])

  tags = {
    Name = each.key
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

data "aws_ssm_parameter" "zone_name" {
  name            = !var.greenfield ? "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain" : "/bfd/platform/network/sensitive/route53/zone/root/domain"
  with_decryption = true
}

data "aws_route53_zone" "root" {
  name         = nonsensitive(data.aws_ssm_parameter.zone_name.value)
  private_zone = true
  tags = {
    "ConfigId" = "root"
  }
}
