#
# Create the DNS records for migration between HealthApt and CCS
#
# This records will be public, but they will contain private IPs
#
locals {
  env           = var.env_config.env
  env_config    = {env=local.env, tags=var.env_config.tags, vpc_id=data.aws_vpc.main.id}
  parent        = "bfdcloud.net"
  name          = "${local.env}.${local.parent}"

  # Health Apt ELB information
  health_apt    = { 
    prod        = {dns="internal-pdcw10lb01-1951212262.us-east-1.elb.amazonaws.com", zone_id="Z35SXDOTRQ7X7K"},
    prod-sbx    = {dns="internal-dpcwelb01-2074070868.us-east-1.elb.amazonaws.com", zone_id="Z35SXDOTRQ7X7K"},
    test        = {dns="internal-tsbb10lb01-758855236.us-east-1.elb.amazonaws.com", zone_id="Z35SXDOTRQ7X7K"}
  }
}

# VPC
#
data "aws_vpc" "main" {
  filter {
    name = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
  }
}

# FHIR ELB
#
data "aws_lb" "fhir" {
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
  source      = "../resources/dns"
  name        = local.env
  parent      = {name=data.aws_route53_zone.parent.name, zone_id=data.aws_route53_zone.parent.zone_id}
  public      = true
  env_config  = local.env_config

  # The apex record just goes the FHIR server
  apex_record = {
    alias         = data.aws_lb.fhir.dns_name 
    zone_id       = data.aws_lb.fhir.zone_id
    health_check  = false
  }

  # The health-apt record goes to the health apt service
  a_records   = [{
    name          = "health-apt"
    alias         = local.health_apt[local.env].dns
    zone_id       = local.health_apt[local.env].zone_id
    health_check  = false
  }]

  # Create one pair of records per partner
  weighted_pairs = [
    { 
      name        = "bb", 
      weight      = var.bb
      a_record    = local.name
      a_set       = "ccs"
      b_record    = "health-apt.${local.name}"
      b_set       = "health-apt"
    },
    { 
      name        = "bcda", 
      weight      = var.bcda
      a_record    = local.name
      a_set       = "ccs"
      b_record    = "health-apt.${local.name}"
      b_set       = "health-apt"
    },
    { 
      name        = "dpc", 
      weight      = var.dpc
      a_record    = local.name
      a_set       = "ccs"
      b_record    = "health-apt.${local.name}"
      b_set       = "health-apt"
    },
    { 
      name        = "mct", 
      weight      = var.mct
      a_record    = local.name
      a_set       = "ccs"
      b_record    = "health-apt.${local.name}"
      b_set       = "health-apt"
    },
  ]
}


