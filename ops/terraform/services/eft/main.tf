locals {
  account_id = data.aws_caller_identity.current.account_id
  vpc_id     = data.aws_vpc.this.id

  env       = terraform.workspace
  service   = "eft"
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  default_tags = {
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Environment    = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/${local.service}"
    Layer          = local.layer
    role           = local.service
  }

  nonsensitive_common_map = zipmap(
    data.aws_ssm_parameters_by_path.nonsensitive_common.names,
    nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values)
  )
  nonsensitive_common_config = {
    for key, value in local.nonsensitive_common_map
    : split("/", key)[5] => value
  }
  sensitive_service_map = zipmap(
    data.aws_ssm_parameters_by_path.sensitive_service.names,
    nonsensitive(data.aws_ssm_parameters_by_path.sensitive_service.values)
  )
  sensitive_service_config = {
    for key, value in local.sensitive_service_map : split("/", key)[5] => value
  }

  vpc_name = local.nonsensitive_common_config["vpc_name"]

  subnet_ip_reservations = jsondecode(
    local.sensitive_service_config["subnet_to_ip_reservations_nlb_json"]
  )

  sftp_port = 22

  # For some reason, the transfer server endpoint service does not support us-east-1b and instead
  # opts to support us-east-1d. In order to enable support for this sub-az in the future
  # automatically (if transfer server VPC endpoints begin to support 1c), we filter our desired
  # subnets against the supported availability zones taking only those that belong to supported azs
  available_endpoint_azs = setintersection(
    data.aws_vpc_endpoint_service.transfer_server.availability_zones,
    values(data.aws_subnet.this)[*].availability_zone
  )
  available_endpoint_subnets = [
    for subnet in values(data.aws_subnet.this)
    : subnet if contains(local.available_endpoint_azs, subnet.availability_zone)
  ]
}

resource "aws_ec2_subnet_cidr_reservation" "this" {
  for_each = local.subnet_ip_reservations

  cidr_block       = "${each.value}/32"
  reservation_type = "explicit"
  subnet_id        = data.aws_subnet.this[each.key].id
}

resource "aws_lb" "this" {
  name               = "${local.full_name}-nlb"
  internal           = true
  load_balancer_type = "network"
  tags               = { Name = "${local.full_name}-nlb" }

  dynamic "subnet_mapping" {
    for_each = local.subnet_ip_reservations

    content {
      subnet_id            = data.aws_subnet.this[subnet_mapping.key].id
      private_ipv4_address = subnet_mapping.value
    }
  }
}

resource "aws_security_group" "this" {
  name        = "${local.full_name}-nlb"
  description = "Allow access to the ${local.service} network load balancer"
  vpc_id      = local.vpc_id
  tags        = { Name = "${local.full_name}-nlb" }

  ingress {
    from_port       = local.sftp_port
    to_port         = local.sftp_port
    protocol        = "tcp"
    cidr_blocks     = [data.aws_vpc.this.cidr_block] # TODO: Determine full CIDR in BFD-2561
    description     = "Allow ingress from SFTP traffic"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.vpn.id]
  }

  egress {
    from_port   = local.sftp_port # TODO: Determine correct port in BFD-2561
    to_port     = local.sftp_port # TODO: Determine correct port in BFD-2561
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.this.cidr_block]
  }
}

resource "aws_transfer_server" "this" {
  domain        = "S3"
  endpoint_type = "VPC_ENDPOINT"
  # host_key               = "" # TODO: Provide host key via SSM
  identity_provider_type = "SERVICE_MANAGED"
  logging_role           = aws_iam_role.logs.arn
  protocols = [
    "SFTP",
  ]
  security_policy_name = "TransferSecurityPolicy-2020-06"
  tags                 = { Name = "${local.full_name}-sftp" }

  endpoint_details {
    vpc_endpoint_id = aws_vpc_endpoint.this.id
  }

  protocol_details {
    passive_ip                  = "AUTO"
    set_stat_option             = "DEFAULT"
    tls_session_resumption_mode = "ENFORCED"
  }
}

resource "aws_vpc_endpoint" "this" {
  ip_address_type = "ipv4"

  private_dns_enabled = false
  security_group_ids = [
    "sg-0a7fae0583d971701", # TODO: Create SG for NLB -> SG -> Transfer Server
  ]
  service_name      = "com.amazonaws.us-east-1.transfer.server"
  subnet_ids        = local.available_endpoint_subnets[*].id
  vpc_endpoint_type = "Interface"
  vpc_id            = local.vpc_id
  tags              = { Name = "${local.full_name}-sftp-endpoint" }

  dns_options {
    dns_record_ip_type = "ipv4"
  }
}
