data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}

data "aws_kms_key" "config_cmk" {
  key_id = local.kms_config_key_alias
}

data "aws_ssm_parameters_by_path" "params" {
  for_each = toset(local.ssm_hierarchies)

  recursive       = true
  path            = each.value
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
    values = [local.vpc_name]
  }
}

# data "aws_subnet" "this" {
#   for_each = local.subnet_ip_reservations

#   vpc_id = local.vpc_id
#   filter {
#     name   = "tag:Name"
#     values = [each.key]
#   }
# }

data "aws_ssm_parameter" "zone_name" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_domain"
  with_decryption = true
}

data "aws_ssm_parameter" "zone_is_private" {
  name            = "/bfd/mgmt/common/sensitive/r53_hosted_zone_root_is_private"
  with_decryption = true
}

data "aws_route53_zone" "this" {
  name         = nonsensitive(data.aws_ssm_parameter.zone_name.value)
  private_zone = nonsensitive(data.aws_ssm_parameter.zone_is_private.value)
  # tags = {
  #   "ConfigId" = nonsensitive(data.aws_ssm_parameter.zone_name.value)
  # }
}
