locals {
  hosted_zones = {
    for zone in jsondecode(local.ssm_config["/bfd/common/r53_hosted_zones_json"]) :
    zone => {
      domain  = local.ssm_config["/bfd/common/r53_hosted_zone_${zone}_domain"]
      comment = local.ssm_config["/bfd/common/r53_hosted_zone_${zone}_comment"]
      # If a hosted zone does not specify any VPC associations, it is considered a Public zone. If
      # any VPCs are specified, it is considered Private. We handle the case where VPCs are not
      # specified in configuration by returning an empty list.
      internal_vpc_ids = jsondecode(lookup(local.ssm_config, "/bfd/common/r53_hosted_zone_${zone}_internal_vpcs_json", "[]"))
      external_vpc_ids = jsondecode(lookup(local.ssm_config, "/bfd/common/r53_hosted_zone_${zone}_external_vpcs_json", "[]"))
    }
  }
  all_internal_r53_vpcs = flatten([for hz_label, hz_config in local.hosted_zones : hz_config.internal_vpc_ids])
}

data "aws_vpc" "internal_r53_hz_vpcs" {
  for_each = toset(local.all_internal_r53_vpcs)

  id = each.key
}

resource "aws_route53_zone" "zones" {
  for_each = local.hosted_zones

  name    = each.value.domain
  comment = each.value.comment

  tags = {
    "ConfigId" = each.key
  }

  dynamic "vpc" {
    for_each = toset(each.value.internal_vpc_ids)

    content {
      vpc_id = data.aws_vpc.internal_r53_hz_vpcs[vpc.key].id
    }
  }

  dynamic "vpc" {
    for_each = toset(each.value.external_vpc_ids)

    content {
      vpc_id = vpc.key
    }
  }
}

# FUTURE: Remove this, it may be unused
resource "aws_route53_zone" "zone" {
  comment = "Managed by Terraform"
  name    = "bfd-mgmt.local"


  vpc {
    vpc_id     = data.aws_vpc.main.id
    vpc_region = "us-east-1"
  }
}
