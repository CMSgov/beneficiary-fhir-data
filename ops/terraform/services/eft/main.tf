locals {
  account_id = data.aws_caller_identity.current.account_id
  vpc_id     = data.aws_vpc.this.id

  env              = terraform.workspace
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", local.env))])
  is_ephemeral_env = !(contains(local.established_envs, local.env))

  service   = "eft"
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  default_tags = {
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Environment    = local.seed_env
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

  kms_key_alias = local.nonsensitive_common_config["kms_key_alias"]
  vpc_name      = local.nonsensitive_common_config["vpc_name"]

  subnet_ip_reservations = jsondecode(
    local.sensitive_service_config["subnet_to_ip_reservations_nlb_json"]
  )
  host_key              = local.sensitive_service_config["sftp_transfer_server_host_private_key"]
  eft_user_sftp_pub_key = local.sensitive_service_config["sftp_eft_user_public_key"]
  eft_user_username     = local.sensitive_service_config["sftp_eft_user_username"]
  eft_bucket_partners   = jsondecode(local.sensitive_service_config["partners_with_bucket_access_json"])
  eft_bucket_partners_iam = {
    for partner in local.eft_bucket_partners :
    partner => {
      bucket_iam_assumer_arn = local.sensitive_service_config["partner_iam_assumer_arn_${partner}"]
      bucket_home_path       = trim(local.sensitive_service_config["partner_bucket_home_path_${partner}"], "/")
    }
  }

  kms_key_id     = data.aws_kms_key.cmk.arn
  sftp_port      = 22
  logging_bucket = "bfd-${local.seed_env}-logs-${local.account_id}"

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

resource "aws_s3_bucket" "this" {
  bucket = local.full_name
  tags   = { Name = local.full_name }
}

resource "aws_s3_bucket_policy" "this" {
  bucket = aws_s3_bucket.this.id
  policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Sid       = "AllowSSLRequestsOnly",
          Effect    = "Deny",
          Principal = "*",
          Action    = "s3:*",
          Resource = [
            "${aws_s3_bucket.this.arn}",
            "${aws_s3_bucket.this.arn}/*"
          ],
          Condition = {
            Bool = {
              "aws:SecureTransport" = "false"
            }
          }
        }
      ]
    }
  )
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = local.kms_key_id
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_logging" "this" {
  bucket = aws_s3_bucket.this.id

  target_bucket = local.logging_bucket
  target_prefix = "${local.full_name}_s3_access_logs/"
}

resource "aws_ec2_subnet_cidr_reservation" "this" {
  for_each = local.subnet_ip_reservations

  cidr_block       = "${each.value}/32"
  reservation_type = "explicit"
  subnet_id        = data.aws_subnet.this[each.key].id
}

resource "aws_lb" "this" {
  name                             = "${local.full_name}-nlb"
  internal                         = true
  enable_cross_zone_load_balancing = true
  load_balancer_type               = "network"
  tags                             = { Name = "${local.full_name}-nlb" }

  dynamic "subnet_mapping" {
    for_each = local.subnet_ip_reservations

    content {
      subnet_id            = data.aws_subnet.this[subnet_mapping.key].id
      private_ipv4_address = subnet_mapping.value
    }
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
  for_each = toset(values(data.aws_network_interface.vpc_endpoint)[*].private_ip)

  target_group_arn = aws_lb_target_group.nlb_to_vpc_endpoint.arn
  target_id        = each.key
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
    cidr_blocks     = [data.aws_vpc.this.cidr_block]
    description     = "Allow ingress from SFTP traffic"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.vpn.id]
  }

  egress {
    from_port   = local.sftp_port
    to_port     = local.sftp_port
    protocol    = "tcp"
    cidr_blocks = [for ip in values(data.aws_network_interface.vpc_endpoint)[*].private_ip : "${ip}/32"]
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

resource "aws_transfer_server" "this" {
  domain                 = "S3"
  endpoint_type          = "VPC_ENDPOINT"
  host_key               = local.host_key
  identity_provider_type = "SERVICE_MANAGED"
  logging_role           = aws_iam_role.logs.arn
  protocols              = ["SFTP"]
  security_policy_name   = "TransferSecurityPolicy-2020-06"
  tags                   = { Name = "${local.full_name}-sftp" }

  endpoint_details {
    vpc_endpoint_id = aws_vpc_endpoint.this.id
  }
}

resource "aws_transfer_user" "eft_user" {
  server_id = aws_transfer_server.this.id
  role      = aws_iam_role.eft_user.arn
  user_name = local.eft_user_username
  tags      = { Name = "${local.full_name}-sftp-user-${local.eft_user_username}" }

  home_directory_type = "LOGICAL"

  home_directory_mappings {
    entry  = "/"
    target = "/${aws_s3_bucket.this.id}/${local.eft_user_username}"
  }
}

resource "aws_transfer_ssh_key" "eft_user" {
  depends_on = [
    aws_transfer_user.eft_user
  ]

  server_id = aws_transfer_server.this.id
  user_name = aws_transfer_user.eft_user.user_name
  body      = local.eft_user_sftp_pub_key
}

resource "aws_vpc_endpoint" "this" {
  ip_address_type = "ipv4"

  private_dns_enabled = false
  security_group_ids  = [aws_security_group.vpc_endpoint.id]
  service_name        = "com.amazonaws.us-east-1.transfer.server"
  subnet_ids          = local.available_endpoint_subnets[*].id
  vpc_endpoint_type   = "Interface"
  vpc_id              = local.vpc_id
  tags                = { Name = "${local.full_name}-sftp-endpoint" }
}
