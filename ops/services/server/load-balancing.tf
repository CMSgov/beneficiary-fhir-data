locals {
  lb_blue_is_public             = nonsensitive(tobool(local.ssm_config["/bfd/${local.service}/lb_is_public"]))
  lb_internal_vpc_peering_cidrs = [for peer, conf in data.aws_vpc_peering_connection.peers : conf.peer_cidr_block]
  lb_internal_vpcs_cidrs        = [data.aws_vpc.mgmt.cidr_block, data.aws_vpc.main.cidr_block]
  lb_internal_ingress_cidrs     = concat(local.lb_internal_vpc_peering_cidrs, local.lb_internal_vpcs_cidrs)
  lb_internal_ingress_pl_ids    = [data.aws_ec2_managed_prefix_list.vpn.id, data.aws_ec2_managed_prefix_list.jenkins.id]
  lb_ingress_port               = 443
  lb_protocol                   = "TCP"
  lb_name_prefix                = "${local.name_prefix}-nlb"
  lbs = {
    "${local.green_state}" = {
      name     = "${local.lb_name_prefix}-${local.green_state}"
      internal = true # green is always internal, regardless of whether blue is public
      subnets  = local.app_subnet_ids
      ingress = {
        cidrs        = local.lb_internal_ingress_cidrs
        prefix_lists = local.lb_internal_ingress_pl_ids
      }
    }
    "${local.blue_state}" = {
      name     = "${local.lb_name_prefix}-${local.blue_state}"
      internal = !local.lb_blue_is_public
      subnets  = !local.lb_blue_is_public ? local.app_subnet_ids : local.dmz_subnet_ids
      ingress = {
        cidrs        = !local.lb_blue_is_public ? local.lb_internal_ingress_cidrs : ["0.0.0.0/0"]
        prefix_lists = !local.lb_blue_is_public ? local.lb_internal_ingress_pl_ids : []
      }
    }
  }
}

resource "aws_lb" "main" {
  for_each = local.lbs

  name                             = each.value.name
  internal                         = each.value.internal
  load_balancer_type               = "network"
  security_groups                  = [aws_security_group.lb[each.key].id]
  subnets                          = each.value.subnets
  enable_deletion_protection       = !local.is_ephemeral_env
  idle_timeout                     = 60
  ip_address_type                  = "ipv4"
  enable_http2                     = false
  desync_mitigation_mode           = "strictest"
  enable_cross_zone_load_balancing = true
}

resource "aws_lb_listener" "main" {
  for_each = local.lbs

  load_balancer_arn = aws_lb.main[each.key].arn
  port              = local.lb_ingress_port
  protocol          = local.lb_protocol

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.main[each.key].arn
  }
}

# security group
resource "aws_security_group" "lb" {
  for_each = local.lbs
  lifecycle {
    create_before_destroy = true
  }

  name        = "${each.value.name}-sg"
  description = "Allow ${each.value.internal ? "internal" : "public"} ingress to the ${each.value.name} NLB; egress to ${local.service} ECS Service containers"
  vpc_id      = data.aws_vpc.main.id
  tags        = merge({ Name = "${each.value.name}-sg" })

  ingress {
    from_port   = local.lb_ingress_port
    to_port     = local.lb_ingress_port
    protocol    = local.lb_protocol
    cidr_blocks = each.value.ingress.cidrs
  }

  # Dynamically create ingress rule for Prefix Lists iff they are specified
  dynamic "ingress" {
    for_each = length(each.value.ingress.prefix_lists) > 0 ? [1] : []
    content {
      from_port       = local.lb_ingress_port
      to_port         = local.lb_ingress_port
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

resource "aws_lb_target_group" "main" {
  for_each = local.lbs

  name                   = "${aws_lb.main[each.key].name}-tg"
  port                   = local.server_port
  protocol               = upper(local.server_protocol)
  vpc_id                 = data.aws_vpc.main.id
  deregistration_delay   = 60
  connection_termination = true
  target_type            = "ip"
  health_check {
    healthy_threshold   = 3
    interval            = 10
    timeout             = 8
    unhealthy_threshold = 2
    port                = local.server_port
    protocol            = upper(local.server_protocol)
  }
}
