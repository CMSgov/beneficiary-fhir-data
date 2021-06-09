# Create a zone, public or private
resource "aws_route53_zone" "main" {
  name          = var.public ? var.parent != null ? "${var.name}.${var.parent.name}" : var.name : "bfd-${var.env_config.env}.local"
  comment       = var.public ? "BFD public zone for ${var.env_config.env}." : "BFD private zone for ${var.env_config.env}."
  tags          = var.env_config.tags
  force_destroy = true

  # VPC is only valid for private zones
  dynamic "vpc" {
    for_each = var.public ? [] : ["dummy"]
    content {
      vpc_id = var.public ? null : var.env_config.vpc_id
    }
  }
}

# Create a NS record that points to the main zone in the parent zone
resource "aws_route53_record" "parent" {
  count   = var.parent != null ? 1 : 0
  zone_id = var.parent.zone_id
  name    = var.name
  type    = "NS"
  ttl     = 300
  records = aws_route53_zone.main.name_servers[*]
}

# Create an A apex record for the zone
resource "aws_route53_record" "apex" {
  count   = var.apex_record != null ? 1 : 0
  zone_id = aws_route53_zone.main.zone_id
  name    = ""
  type    = "A"

  alias {
    name                   = var.apex_record.alias
    zone_id                = var.apex_record.zone_id
    evaluate_target_health = true
  }
}

# Create A-records for the zone
resource "aws_route53_record" "a" {
  count   = length(var.a_records)
  zone_id = aws_route53_zone.main.zone_id
  name    = var.a_records[count.index].name
  type    = "A"

  alias {
    name                   = var.a_records[count.index].alias
    zone_id                = var.a_records[count.index].zone_id
    evaluate_target_health = true
  }
}
