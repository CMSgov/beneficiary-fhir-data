locals {
  lb_blue_is_public             = nonsensitive(tobool(local.ssm_config["/bfd/${local.service}/lb_is_public"]))
  lb_internal_vpc_peering_cidrs = [for connection in module.terraservice.all_connections : connection.foreign_cidr if connection.env == local.parent_env]
  lb_internal_vpcs_cidrs        = !var.greenfield ? [one(data.aws_vpc.mgmt[*].cidr_block), local.vpc.cidr_block] : [local.vpc.cidr_block]
  lb_internal_ingress_cidrs     = concat(local.lb_internal_vpc_peering_cidrs, local.lb_internal_vpcs_cidrs)
  lb_internal_ingress_pl_ids    = concat([data.aws_ec2_managed_prefix_list.vpn.id], data.aws_ec2_managed_prefix_list.jenkins[*].id)
  lb_ingress_port               = 443
  lb_protocol                   = "HTTPS"
  lb_name_prefix                = "${local.name_prefix}-alb"
  lb_subnets                    = !local.lb_blue_is_public ? local.app_subnet_ids : local.dmz_subnet_ids

  listeners = {
    "${local.green_state}" = {
      port = local.server_port
      ingress = {
        cidrs        = local.lb_internal_ingress_cidrs
        prefix_lists = local.lb_internal_ingress_pl_ids
      }
    }
    "${local.blue_state}" = {
      port = 443
      ingress = {
        cidrs        = !local.lb_blue_is_public ? local.lb_internal_ingress_cidrs : ["0.0.0.0/0"]
        prefix_lists = !local.lb_blue_is_public ? local.lb_internal_ingress_pl_ids : []
      }
    }
  }
  listener_host_cert     = nonsensitive(local.ssm_config["/bfd/${local.service}/lb/host_cert"])
  listener_host_cert_key = local.ssm_config["/bfd/${local.service}/lb/host_cert_key"]
}

resource "aws_lb" "this" {
  name                             = local.lb_name_prefix
  internal                         = !local.lb_blue_is_public
  load_balancer_type               = "application"
  security_groups                  = values(aws_security_group.lb)[*].id
  subnets                          = local.lb_subnets
  enable_deletion_protection       = !local.is_ephemeral_env
  idle_timeout                     = 60
  ip_address_type                  = "ipv4"
  enable_http2                     = true
  desync_mitigation_mode           = "strictest"
  enable_cross_zone_load_balancing = true
}

resource "aws_acm_certificate" "this" {
  private_key      = local.listener_host_cert_key
  certificate_body = local.listener_host_cert
}

resource "aws_lb_listener" "this" {
  for_each = local.listeners
  lifecycle {
    # ECS blue/green will swap the target group during a blue/green deployment, so we need to ignore
    # any changes
    ignore_changes = [default_action]
  }

  load_balancer_arn = aws_lb.this.arn
  port              = each.value.port
  protocol          = local.lb_protocol
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-Res-2021-06"
  certificate_arn   = aws_acm_certificate.this.arn

  default_action {
    type             = "forward"
    target_group_arn = each.key == local.green_state ? aws_lb_target_group.this[1].arn : aws_lb_target_group.this[0].arn
  }

  mutual_authentication {
    mode = "passthrough"
  }
}

# This is an unfortunately necessary external data source as ECS Blue/Green requires that a Rule—not
# a listener—ARN must be specified for ECS native Blue/Green deployments and both the
# aws_lb_listener resource and aws_lb_listener_rule data resource do not provide any means of
# getting the default listener rule arn. As of 09/23/25, PR #43941 is open to support
# retrieval of the default rule via the aws_lb_listener_rule data resource, and this should be
# revisited once it's been merged
data "external" "lb_listener_default_rule" {
  for_each = local.listeners

  program = [
    "bash",
    "-c",
    <<EOF
    arn="$(
      aws elbv2 describe-rules --listener-arn "${aws_lb_listener.this[each.key].arn}" \
        --query 'Rules[?IsDefault==`true`].RuleArn' \
        --output text
    )"
    echo "{\"arn\":\"$arn\"}"
    EOF
  ]
}

resource "aws_security_group" "lb" {
  for_each = local.listeners

  name        = "${local.lb_name_prefix}-${each.key}-sg"
  description = "Allow blue/green ingress to the ${local.lb_name_prefix} NLB; egress to ${local.service} ECS Service containers"
  vpc_id      = local.vpc.id
  tags        = merge({ Name = "${local.lb_name_prefix}-${each.key}-sg" })

  ingress {
    from_port   = each.value.port
    to_port     = each.value.port
    protocol    = "TCP"
    cidr_blocks = each.value.ingress.cidrs
  }

  # Dynamically create ingress rule for Prefix Lists iff they are specified
  dynamic "ingress" {
    for_each = length(each.value.ingress.prefix_lists) > 0 ? [1] : []
    content {
      from_port       = each.value.port
      to_port         = each.value.port
      protocol        = "TCP"
      prefix_list_ids = each.value.ingress.prefix_lists
    }
  }

  egress {
    from_port       = local.server_port
    to_port         = local.server_port
    protocol        = "TCP"
    security_groups = [aws_security_group.server.id]
  }
}

resource "aws_lb_target_group" "this" {
  # We don't do a for_each because it would be a misnomer to name the Target Groups "blue" or
  # "green" in a CodeDeploy-based deployment since they are not actually the blue or green
  # resources--the Listeners are.
  count = length(local.listeners)

  name                 = "${local.name_prefix}-tg-${count.index}"
  port                 = local.server_port
  protocol             = "HTTP"
  vpc_id               = local.vpc.id
  deregistration_delay = 30
  target_type          = "ip"
  health_check {
    healthy_threshold   = 2
    interval            = 10
    timeout             = 5
    unhealthy_threshold = 5
    port                = local.server_port
    protocol            = "HTTP"
    path                = "/v3/fhir/metadata"
    matcher             = "200,401"
  }
}

resource "aws_route53_record" "this" {
  name    = "${local.env}.fhirv3.${data.aws_route53_zone.parent_env.name}"
  type    = "A"
  zone_id = data.aws_route53_zone.parent_env.zone_id

  alias {
    name                   = aws_lb.this.dns_name
    zone_id                = aws_lb.this.zone_id
    evaluate_target_health = true
  }
}
