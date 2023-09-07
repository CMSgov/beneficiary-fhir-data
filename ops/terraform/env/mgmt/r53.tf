locals {
  hosted_zones = {
    for zone in jsondecode(local.sensitive_common_config["r53_hosted_zone_labels_json"]) :
    zone => {
      domain  = local.sensitive_common_config["r53_hosted_zone_domain_${zone}"]
      comment = local.sensitive_common_config["r53_hosted_zone_comment_${zone}"]
      # If a hosted zone does not specify any VPC associations, it is considered a Public zone. If
      # any VPCs are specified, it is considered Private. We handle the case where VPCs are not
      # specified in configuration by returning an empty list.
      vpc_ids = jsondecode(lookup(local.sensitive_common_config, "r53_hosted_zone_vpcs_json_${zone}", "[]"))
    }
  }
  all_r53_vpcs = flatten([for hz_label, hz_config in local.hosted_zones : hz_config.vpc_ids])
}

data "aws_vpc" "r53_hz_vpcs" {
  for_each = toset(local.all_r53_vpcs)

  id = each.key
}

resource "aws_route53_zone" "zones" {
  for_each = local.hosted_zones

  name    = each.value.domain
  comment = each.value.comment

  dynamic "vpc" {
    for_each = toset(each.value.vpc_ids)

    content {
      vpc_id = data.aws_vpc.r53_hz_vpcs[vpc.key].id
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
