# Create weighted A-record pairs with 0-100 for weights.
#

resource "aws_route53_record" "record_a" {
  for_each = var.weights
  zone_id  = var.zone_id
  name     = each.key
  type     = "A"

  alias {
    name                   = var.a_alias
    zone_id                = var.a_zone_id
    evaluate_target_health = true
  }

  set_identifier = var.a_set
  weighted_routing_policy {
    weight = min(100, each.value)
  }
}

resource "aws_route53_record" "record_b" {
  for_each = var.weights
  zone_id  = var.zone_id
  name     = each.key
  type     = "A"

  alias {
    name                   = var.b_alias
    zone_id                = var.b_zone_id
    evaluate_target_health = true
  }

  set_identifier = var.b_set
  weighted_routing_policy {
    weight = max(0, 100 - each.value)
  }
}
