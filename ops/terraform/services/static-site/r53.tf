## This resource is currently commented out intentionally during what has been characterized as Proof of Concept investigation.
## If CA-signed certificates for the private domain are deemed necessary, inclusion of those certificates within the configuration 
## will trigger enabling this resource for private domain aliasing.
## TODO
# resource "aws_route53_record" "static_env" {
#   zone_id = data.aws_route53_zone.vpc_root.zone_id
#   name    = local.static_site_fqdn
#   type    = "A"

#   alias {
#     name    = aws_cloudfront_distribution.static_site_distribution.domain_name
#     zone_id = aws_cloudfront_distribution.static_site_distribution.hosted_zone_id

#     evaluate_target_health = false # true
#   }

# }
