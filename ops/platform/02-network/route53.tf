locals {
  route53_ssm_path = "/bfd/network/route53"
  hosted_zones = {
    for zone in jsondecode(nonsensitive(local.ssm_config["${local.route53_ssm_path}/zones_list_json"])) :
    zone => {
      domain  = nonsensitive(local.ssm_config["${local.route53_ssm_path}/zone/${zone}/domain"])
      comment = nonsensitive(local.ssm_config["${local.route53_ssm_path}/zone/${zone}/comment"])
      # If a hosted zone does not specify any VPC associations, it is considered a Public zone. If
      # any VPCs are specified, it is considered Private. We do not want to configure any
      # internet-facing, public Route53 Hosted Zones, so returning an empty list will result in an
      # error
      internal_vpc_ids = jsondecode(nonsensitive(local.ssm_config["${local.route53_ssm_path}/zone/${zone}/internal_vpcs_list_json"]))
      external_vpc_ids = jsondecode(nonsensitive(lookup(local.ssm_config, "${local.route53_ssm_path}/zone/${zone}/external_vpcs_list_json", "[]")))
      records          = jsondecode(nonsensitive(lookup(local.ssm_config, "${local.route53_ssm_path}/zone/${zone}/records", "[]")))
    }
  }
  all_internal_r53_vpcs = flatten(values(local.hosted_zones)[*].internal_vpc_ids)
}

data "aws_vpc" "internal_r53_hz_vpcs" {
  for_each = toset(local.all_internal_r53_vpcs)

  id = each.key
}

resource "aws_route53_zone" "main" {
  lifecycle {
    precondition {
      condition     = alltrue([for hz in values(local.hosted_zones) : length(hz.internal_vpc_ids) > 0])
      error_message = "All configured Hosted Zones must have internal VPCs associated with them; public HZs are unsupported."
    }
  }

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

resource "aws_route53_record" "main" {
  for_each = {
    for val in [
      for zone_name, zone_config in local.hosted_zones :
      [
        for record in zone_config.records :
        {
          record = record
          zone   = zone
        }
      ]
    ] : "${val.zone}_${val.record.name}" => val
  }

  zone_id = aws_route53_zone.main[each.value.zone].zone_id

  name    = each.value.record.name
  type    = each.value.record.type
  ttl     = each.value.record.ttl
  records = [each.value.record.value]
}
