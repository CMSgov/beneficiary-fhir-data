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

  sensitive_service_map = zipmap(
    data.aws_ssm_parameters_by_path.sensitive_service.names,
    nonsensitive(data.aws_ssm_parameters_by_path.sensitive_service.values)
  )
  sensitive_service_config = {
    for key, value in local.sensitive_service_map : split("/", key)[5] => value
  }

  subnet_ip_reservations = jsondecode(
    local.sensitive_service_config["subnet_to_ip_reservations_nlb_json"]
  )

  sftp_port = 22
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

  access_logs {
    enabled = true
    bucket  = data.aws_s3_bucket.logs.id
    prefix  = "${replace(local.full_name, "-", "_")}_nlb"
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

resource "aws_s3_bucket_policy" "logs" {
  bucket = data.aws_s3_bucket.logs.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "EFTNLBAccessLogs",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
          "AWS": "${data.aws_elb_service_account.this.arn}"
      },
      "Action": "s3:PutObject",
      "Resource": "${data.aws_s3_bucket.logs.arn}/${local.full_name}*"
    }
  ]
}
POLICY
}

