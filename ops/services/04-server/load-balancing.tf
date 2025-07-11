locals {
  lb_blue_is_public             = nonsensitive(tobool(local.ssm_config["/bfd/${local.service}/lb_is_public"]))
  lb_internal_vpc_peering_cidrs = [for connection in module.terraservice.all_connections : connection.foreign_cidr if connection.env == local.parent_env]
  lb_internal_vpcs_cidrs        = !var.greenfield ? [one(data.aws_vpc.mgmt[*].cidr_block), local.vpc.cidr_block] : [local.vpc.cidr_block]
  lb_internal_ingress_cidrs     = concat(local.lb_internal_vpc_peering_cidrs, local.lb_internal_vpcs_cidrs)
  lb_internal_ingress_pl_ids    = concat([data.aws_ec2_managed_prefix_list.vpn.id], data.aws_ec2_managed_prefix_list.jenkins[*].id)
  lb_ingress_port               = 443
  lb_protocol                   = "TCP"
  lb_name_prefix                = "${local.name_prefix}-nlb"
  lb_subnets                    = !local.lb_blue_is_public ? local.app_subnet_ids : local.dmz_subnet_ids
  listeners = {
    # TODO: Fundamentally incompatible with public LBs; need to figure out how to reconcile with port rules
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
}

resource "aws_lb" "this" {
  name                             = local.lb_name_prefix
  internal                         = !local.lb_blue_is_public
  load_balancer_type               = "network"
  security_groups                  = values(aws_security_group.lb)[*].id
  subnets                          = local.lb_subnets
  enable_deletion_protection       = !local.is_ephemeral_env
  idle_timeout                     = 60
  ip_address_type                  = "ipv4"
  enable_http2                     = false
  desync_mitigation_mode           = "strictest"
  enable_cross_zone_load_balancing = true
}

resource "aws_lb_listener" "this" {
  for_each = local.listeners
  lifecycle {
    # CodeDeploy will swap the target group during a blue/green deployment, so we need to ignore any
    # changes
    ignore_changes = [default_action]
  }

  load_balancer_arn = aws_lb.this.arn
  port              = each.value.port
  protocol          = local.lb_protocol

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this[0].arn
  }
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
    protocol    = local.lb_protocol
    cidr_blocks = each.value.ingress.cidrs
  }

  # Dynamically create ingress rule for Prefix Lists iff they are specified
  dynamic "ingress" {
    for_each = length(each.value.ingress.prefix_lists) > 0 ? [1] : []
    content {
      from_port       = each.value.port
      to_port         = each.value.port
      protocol        = local.lb_protocol
      prefix_list_ids = each.value.ingress.prefix_lists
    }
  }

  egress {
    from_port       = local.server_port
    to_port         = local.server_port
    protocol        = local.lb_protocol
    security_groups = [aws_security_group.server.id]
  }
}

resource "aws_lb_target_group" "this" {
  # We don't do a for_each because it would be a misnomer to name the Target Groups "blue" or
  # "green" in a CodeDeploy-based deployment since they are not actually the blue or green
  # resources--the Listeners are.
  count = length(local.listeners)

  name                   = "${local.name_prefix}-tg-${count.index}"
  port                   = local.server_port
  protocol               = upper(local.server_protocol)
  vpc_id                 = local.vpc.id
  deregistration_delay   = 30
  connection_termination = true
  target_type            = "ip"
  health_check {
    healthy_threshold   = 2
    interval            = 5
    timeout             = 5
    unhealthy_threshold = 5
    port                = local.server_port
    protocol            = upper(local.server_protocol)
  }
}

resource "aws_route53_record" "this" {
  count = local.root_zone_configured ? 1 : 0

  name    = "${local.env}.fhir.${one(data.aws_route53_zone.root[*].name)}"
  type    = "A"
  zone_id = one(data.aws_route53_zone.root[*].zone_id)

  alias {
    name                   = aws_lb.this.dns_name
    zone_id                = aws_lb.this.zone_id
    evaluate_target_health = true
  }
}
