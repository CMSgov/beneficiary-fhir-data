#
# Create the DNS records for migration between HealthApt and CCS
#
# This records will be public, but they will contain private IPs
#
locals {
  env        = var.env_config.env
  env_config = { env = local.env, tags = var.env_config.tags, vpc_id = data.aws_vpc.main.id }
  parent     = "bfdcloud.net"
  name       = "${local.env}.${local.parent}"

  # Health Apt ELB information
  health_apt = {
    prod     = { dns = "internal-pdcw10lb01-1951212262.us-east-1.elb.amazonaws.com", zone_id = "Z35SXDOTRQ7X7K" },
    prod-sbx = { dns = "internal-dpcwelb01-2074070868.us-east-1.elb.amazonaws.com", zone_id = "Z35SXDOTRQ7X7K" },
    test     = { dns = "internal-tsbb10lb01-758855236.us-east-1.elb.amazonaws.com", zone_id = "Z35SXDOTRQ7X7K" }
  }
}

# VPC
#
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
  }
}

# FHIR ELB
#
data "aws_elb" "fhir" {
  name = "bfd-${local.env}-fhir"
}

# Parent zone
#
data "aws_route53_zone" "parent" {
  name = local.parent
}

# DNS zone and records
#
module "main" {
  source     = "../resources/dns"
  name       = local.env
  parent     = { name = data.aws_route53_zone.parent.name, zone_id = data.aws_route53_zone.parent.zone_id }
  public     = true
  env_config = local.env_config

  # The apex record just goes the FHIR server
  apex_record = {
    alias   = data.aws_elb.fhir.dns_name
    zone_id = data.aws_elb.fhir.zone_id
  }

  # The health-apt record goes to the health apt service
  a_records = [{
    name    = "health-apt"
    alias   = local.health_apt[local.env].dns
    zone_id = local.health_apt[local.env].zone_id
  }]
}

# Create a CNAME pair
#
module "weighted_pairs" {
  source     = "../resources/dns_pairs"
  env_config = local.env_config
  zone_id    = module.main.zone_id

  a_set     = "ccs"
  a_alias   = data.aws_elb.fhir.dns_name
  a_zone_id = data.aws_elb.fhir.zone_id

  b_set     = "health-apt"
  b_alias   = local.health_apt[local.env].dns
  b_zone_id = local.health_apt[local.env].zone_id

  # Create one pair per partner
  weights = {
    bb   = var.bb,
    bcda = var.bcda,
    dpc  = var.dpc,
    mct  = var.mct
  }
}


