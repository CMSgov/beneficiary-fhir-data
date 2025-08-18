data "aws_iam_policy_document" "topic_inbound_received_s3_notifs" {
  for_each = toset(local.eft_partners_with_inbound_received_notifs)

  statement {
    sid       = "Allow_Publish_from_S3"
    actions   = ["SNS:Publish"]
    resources = [local.topic_arn_placeholder]

    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = ["${module.bucket_eft.bucket.arn}"]
    }
  }

  dynamic "statement" {
    for_each = {
      for index, target in local.eft_partners_config[each.key].inbound.s3_notifications.received_file_targets
      : index => target
    }
    content {
      sid       = "Allow_Subscribe_from_${each.key}_${statement.key}"
      actions   = ["SNS:Subscribe", "SNS:Receive"]
      resources = [local.topic_arn_placeholder]

      principals {
        type        = "AWS"
        identifiers = [statement.value.principal]
      }

      condition {
        test     = "StringEquals"
        variable = "sns:Protocol"
        values   = [statement.value.protocol]

      }

      condition {
        test     = "ForAllValues:StringEquals"
        variable = "sns:Endpoint"
        values   = [statement.value.arn]
      }
    }
  }
}

module "topic_inbound_received_s3_notifs" {
  for_each = toset(local.eft_partners_with_inbound_received_notifs)

  source = "../../terraform-modules/general/logging-sns-topic"

  topic_name                   = "${local.full_name}-inbound-received-s3-${each.key}"
  additional_topic_policy_docs = [data.aws_iam_policy_document.topic_inbound_received_s3_notifs[each.key].json]

  iam_path                 = local.iam_path
  permissions_boundary_arn = local.permissions_boundary_arn

  kms_key_arn = local.env_key_arn

  sqs_sample_rate    = 100
  lambda_sample_rate = 100
}

resource "aws_ec2_subnet_cidr_reservation" "this" {
  for_each = local.subnet_ip_reservations

  cidr_block       = "${each.value}/32"
  reservation_type = "explicit"
  subnet_id        = one([for subnet in local.subnets : subnet.id if subnet.tags["Name"] == each.key])

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_lb" "this" {
  name                             = "${local.full_name}-nlb"
  internal                         = true
  enable_cross_zone_load_balancing = true
  load_balancer_type               = "network"
  tags                             = { Name = "${local.full_name}-nlb" }

  dynamic "subnet_mapping" {
    for_each = local.available_endpoint_subnets

    content {
      subnet_id            = subnet_mapping.value.id
      private_ipv4_address = local.subnet_ip_reservations[subnet_mapping.value.tags["Name"]]
    }
  }
}

resource "aws_route53_record" "nlb_alias" {
  name    = "${local.env}.${local.service}.${data.aws_route53_zone.this.name}"
  type    = "A"
  zone_id = data.aws_route53_zone.this.zone_id

  alias {
    name                   = aws_lb.this.dns_name
    zone_id                = aws_lb.this.zone_id
    evaluate_target_health = true
  }
}

resource "aws_lb_target_group" "nlb_to_vpc_endpoint" {
  name            = "${local.full_name}-nlb-to-vpce"
  port            = local.sftp_port
  protocol        = "TCP"
  target_type     = "ip"
  ip_address_type = "ipv4"
  vpc_id          = local.vpc_id
  tags            = { Name = "${local.full_name}-nlb-to-vpce" }
}

resource "aws_alb_target_group_attachment" "nlb_to_vpc_endpoint" {
  count = length(local.available_endpoint_subnets)

  target_group_arn = aws_lb_target_group.nlb_to_vpc_endpoint.arn
  target_id        = data.aws_network_interface.vpc_endpoint[count.index].private_ip
}

resource "aws_lb_listener" "nlb_to_vpc_endpoint" {
  load_balancer_arn = aws_lb.this.arn
  port              = local.sftp_port
  protocol          = "TCP"
  tags              = { Name = "${local.full_name}-nlb-listener" }

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.nlb_to_vpc_endpoint.arn
  }
}

resource "aws_security_group" "nlb" {
  name        = "${local.full_name}-nlb"
  description = "Allow access to the ${local.service} network load balancer"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.full_name}-nlb" }

  ingress {
    from_port       = local.sftp_port
    to_port         = local.sftp_port
    protocol        = "tcp"
    cidr_blocks     = [local.vpc.cidr_block]
    description     = "Allow ingress from SFTP traffic"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.vpn.id]
  }

  egress {
    from_port   = local.sftp_port
    to_port     = local.sftp_port
    protocol    = "tcp"
    cidr_blocks = [for ip in data.aws_network_interface.vpc_endpoint[*].private_ip : "${ip}/32"]
  }
}

resource "aws_security_group" "vpc_endpoint" {
  name        = "${local.full_name}-vpc-endpoint"
  description = "Allow ingress and egress from ${aws_lb.this.name}"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.full_name}-vpc-endpoint" }

  ingress {
    from_port   = local.sftp_port
    to_port     = local.sftp_port
    protocol    = "tcp"
    cidr_blocks = [for ip in aws_lb.this.subnet_mapping[*].private_ipv4_address : "${ip}/32"]
    description = "Allow ingress from SFTP traffic from NLB"
  }

  egress {
    from_port   = local.sftp_port
    to_port     = local.sftp_port
    protocol    = "tcp"
    cidr_blocks = [for ip in aws_lb.this.subnet_mapping[*].private_ipv4_address : "${ip}/32"]
    description = "Allow egress from SFTP traffic from NLB"
  }
}

resource "aws_cloudwatch_log_group" "sftp_server" {
  name         = "/bfd/${local.service}/${local.full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_transfer_server" "this" {
  depends_on = [aws_iam_role_policy_attachment.sftp_server]

  domain                      = "S3"
  endpoint_type               = "VPC_ENDPOINT"
  host_key                    = local.inbound_sftp_server_key
  identity_provider_type      = "SERVICE_MANAGED"
  logging_role                = aws_iam_role.sftp_server.arn
  protocols                   = ["SFTP"]
  security_policy_name        = "TransferSecurityPolicy-FIPS-2024-01"
  structured_log_destinations = [aws_cloudwatch_log_group.sftp_server.arn]
  tags                        = { Name = local.sftp_full_name }

  endpoint_details {
    vpc_endpoint_id = aws_vpc_endpoint.this.id
  }
}

resource "aws_transfer_user" "eft_user" {
  depends_on = [aws_iam_role_policy_attachment.sftp_user]

  server_id = aws_transfer_server.this.id
  role      = aws_iam_role.sftp_user.arn
  user_name = local.inbound_sftp_user_username
  tags      = { Name = "${local.sftp_full_name}-sftp-user" }

  home_directory_type = "LOGICAL"

  home_directory_mappings {
    entry  = "/"
    target = "/${module.bucket_eft.bucket.id}/${local.inbound_sftp_s3_home_dir}"
  }
}

resource "aws_transfer_ssh_key" "eft_user" {
  depends_on = [
    aws_transfer_user.eft_user
  ]

  server_id = aws_transfer_server.this.id
  user_name = aws_transfer_user.eft_user.user_name
  body      = local.inbound_sftp_user_pub_key
}

resource "aws_vpc_endpoint" "this" {
  ip_address_type = "ipv4"

  private_dns_enabled = false
  security_group_ids  = [aws_security_group.vpc_endpoint.id]
  service_name        = data.aws_vpc_endpoint_service.transfer_server.service_name
  subnet_ids          = local.available_endpoint_subnets[*].id
  vpc_endpoint_type   = "Interface"
  vpc_id              = local.vpc_id
  tags                = { Name = "${local.full_name}-sftp-endpoint" }
}
