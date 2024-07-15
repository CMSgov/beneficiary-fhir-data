resource "aws_route53_record" "static_env" {
  zone_id = data.aws_route53_zone.vpc_root.zone_id
  name    = local.static_site_fqdn
  type    = "A"

  alias {
    name    = aws_lb.static_lb.dns_name
    zone_id = data.aws_lb_hosted_zone_id.static_lb.id

    evaluate_target_health = false
  }

}
