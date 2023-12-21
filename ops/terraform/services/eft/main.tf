module "terraservice" {
  source = "../_modules/bfd-terraservice"

  environment_name     = terraform.workspace
  relative_module_root = "ops/terraform/services/eft"
  additional_tags = {
    Layer = local.layer
    Name  = local.full_name
    role  = local.service
  }
}

locals {
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  seed_env         = module.terraservice.seed_env
  is_ephemeral_env = module.terraservice.is_ephemeral_env

  service   = "eft"
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  eft_partners        = jsondecode(nonsensitive(data.aws_ssm_parameter.partners_list_json.value))
  ssm_hierarchy_roots = concat(["bfd"], local.eft_partners)
  ssm_hierarchies = flatten([
    for root in local.ssm_hierarchy_roots :
    ["/${root}/${local.env}/common", "/${root}/${local.env}/${local.service}"]
  ])
  ssm_flattened_data = {
    names = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : v.names]
    )
    values = flatten(
      [for k, v in data.aws_ssm_parameters_by_path.params : nonsensitive(v.values)]
    )
  }
  # This returns an object with keys that follow conventional SSM Parameter naming, _excluding_ the
  # nonsensitve/sensitive node. for example, to get a parameter named "vpc_name" in BFD's hierarchy
  # in the "common" service, it would be: local.ssm_config["/bfd/common/vpc_name"]. Or, if the
  # parameter is something more like /dpc/eft/sensitive/inbound/dir, it'd be like:
  # local.ssm_config["/dpc/eft/inbound/dir"]. Essentially, the environment and sensitivity nodes in
  # a given parameter's path are removed to reduce the verbosity of referencing parameters
  #FUTURE: Refactor something like this out into a distinct module much like bfd-terraservice above
  ssm_config = zipmap(
    [
      for name in local.ssm_flattened_data.names :
      replace(name, "/((non)*sensitive|${local.env})//", "")
    ],
    local.ssm_flattened_data.values
  )

  # SSM Lookup
  kms_key_alias = local.ssm_config["/bfd/common/kms_key_alias"]
  vpc_name      = local.ssm_config["/bfd/common/vpc_name"]

  subnet_ip_reservations = jsondecode(
    local.ssm_config["/bfd/${local.service}/subnet_to_ip_reservations_nlb_json"]
  )
  host_key                = local.ssm_config["/bfd/${local.service}/sftp_transfer_server_host_private_key"]
  eft_r53_hosted_zone     = local.ssm_config["/bfd/${local.service}/r53_hosted_zone"]
  eft_user_sftp_pub_key   = local.ssm_config["/bfd/${local.service}/sftp_eft_user_public_key"]
  eft_user_username       = local.ssm_config["/bfd/${local.service}/sftp_eft_user_username"]
  eft_s3_sftp_home_folder = trim(local.ssm_config["/bfd/${local.service}/sftp_eft_home_dir"], "/")
  eft_partners_config = {
    for partner in local.eft_partners :
    partner => {
      bucket_home_path        = "${local.eft_s3_sftp_home_folder}/${trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/")}"
      bucket_iam_assumer_arns = jsondecode(local.ssm_config["/${partner}/${local.service}/bucket_iam_assumer_arns_json"])
      inbound = {
        dir = join(
          "/",
          [
            local.eft_s3_sftp_home_folder,
            trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
            trim(local.ssm_config["/${partner}/${local.service}/inbound/dir"], "/")
          ]
        )
        s3_notifications = {
          received_file_targets = jsondecode(
            lookup(local.ssm_config, "/${partner}/${local.service}/inbound/s3_notifications/received_file_targets_json", "[]")
          )
        }
      }
      outbound = {
        pending_path = join(
          "/",
          [
            local.eft_s3_sftp_home_folder,
            trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
            trim(local.ssm_config["/${partner}/${local.service}/outbound/pending_dir"], "/")
          ]
        ),
        sent_path = join(
          "/",
          [
            local.eft_s3_sftp_home_folder,
            trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
            trim(local.ssm_config["/${partner}/${local.service}/outbound/sent_dir"], "/")
          ]
        ),
        failed_path = join(
          "/",
          [
            local.eft_s3_sftp_home_folder,
            trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
            trim(local.ssm_config["/${partner}/${local.service}/outbound/failed_dir"], "/")
          ]
        ),
        s3_notifications = {
          pending_file_targets = jsondecode(
            lookup(local.ssm_config, "/${partner}/${local.service}/outbound/s3_notifications/pending_file_targets_json", "[]")
          ),
          sent_file_targets = jsondecode(
            lookup(local.ssm_config, "/${partner}/${local.service}/outbound/s3_notifications/sent_file_targets_json", "[]")
          ),
          failed_file_targets = jsondecode(
            lookup(local.ssm_config, "/${partner}/${local.service}/outbound/s3_notifications/failed_file_targets_json", "[]")
          )
        }
      }
    }
  }
  eft_partners_with_inbound_received_notifs = [
    for partner in local.eft_partners :
    partner if length(local.eft_partners_config[partner].inbound.s3_notifications.received_file_targets) > 0
  ]
  eft_partners_with_outbound_pending_notifs = [
    for partner in local.eft_partners :
    partner if length(local.eft_partners_config[partner].outbound.s3_notifications.pending_file_targets) > 0
  ]
  eft_partners_with_outbound_sent_notifs = [
    for partner in local.eft_partners :
    partner if length(local.eft_partners_config[partner].outbound.s3_notifications.sent_file_targets) > 0
  ]
  eft_partners_with_outbound_failed_notifs = [
    for partner in local.eft_partners :
    partner if length(local.eft_partners_config[partner].outbound.s3_notifications.failed_file_targets) > 0
  ]
  # Data source lookups

  account_id     = data.aws_caller_identity.current.account_id
  vpc_id         = data.aws_vpc.this.id
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
}

resource "aws_s3_bucket_notification" "bucket_notifications" {
  count = length(concat([
    local.eft_partners_with_inbound_received_notifs,
    local.eft_partners_with_outbound_pending_notifs,
    local.eft_partners_with_outbound_sent_notifs,
    local.eft_partners_with_outbound_failed_notifs
  ])) > 0 ? 1 : 0

  bucket = aws_s3_bucket.this.id

  dynamic "topic" {
    # This is pretty strange, but is necessitated by "for_each" requiring either a _set_ of strings
    # or a map, and since partners aren't unique per-target this is the easiest way to workaround
    # "for_each"'s limitations without additional code. The key is completely unnecessary, so the
    # index is used to ensure uniqueness
    for_each = {
      for index, tupl in flatten([
        for partner in local.eft_partners_with_inbound_received_notifs :
        [
          for target in local.eft_partners_config[partner].inbound.s3_notifications.received_file_targets :
          { partner = partner, target = target }
        ]
      ]) : index => tupl
    }

    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.value.partner].inbound.dir}/"
      id            = aws_sns_topic.inbound_received_s3_notifs[topic.value.partner].name
      topic_arn     = aws_sns_topic.inbound_received_s3_notifs[topic.value.partner].arn
    }
  }

  dynamic "topic" {
    for_each = {
      for index, tupl in flatten([
        for partner in local.eft_partners_with_outbound_pending_notifs :
        [
          for target in local.eft_partners_config[partner].outbound.s3_notifications.pending_file_targets :
          { partner = partner, target = target }
        ]
      ]) : index => tupl
    }

    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.value.partner].outbound.pending_path}/"
      id            = aws_sns_topic.outbound_pending_s3_notifs[topic.value.partner].name
      topic_arn     = aws_sns_topic.outbound_pending_s3_notifs[topic.value.partner].arn
    }
  }

  dynamic "topic" {
    for_each = {
      for index, tupl in flatten([
        for partner in local.eft_partners_with_outbound_sent_notifs :
        [
          for target in local.eft_partners_config[partner].outbound.s3_notifications.sent_file_targets :
          { partner = partner, target = target }
        ]
      ]) : index => tupl
    }

    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.value.partner].outbound.sent_path}/"
      id            = aws_sns_topic.outbound_sent_s3_notifs[topic.value.partner].name
      topic_arn     = aws_sns_topic.outbound_sent_s3_notifs[topic.value.partner].arn
    }
  }

  dynamic "topic" {
    for_each = {
      for index, tupl in flatten([
        for partner in local.eft_partners_with_outbound_sent_notifs :
        [
          for target in local.eft_partners_config[partner].outbound.s3_notifications.failed_file_targets :
          { partner = partner, target = target }
        ]
      ]) : index => tupl
    }

    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.value.partner].outbound.failed_path}/"
      id            = aws_sns_topic.outbound_failed_s3_notifs[topic.value.partner].name
      topic_arn     = aws_sns_topic.outbound_failed_s3_notifs[topic.value.partner].arn
    }
  }
}

# FUTURE: If any additional SNS Topics are required similar to the ones defined below, this
# duplication should be consolidated
resource "aws_sns_topic" "inbound_received_s3_notifs" {
  for_each = toset(local.eft_partners_with_inbound_received_notifs)

  name              = "${local.full_name}-inbound-received-s3-${each.key}"
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "inbound_received_s3_notifs" {
  for_each = toset(local.eft_partners_with_inbound_received_notifs)

  arn = aws_sns_topic.inbound_received_s3_notifs[each.key].arn
  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = concat(
        [
          {
            Sid       = "Allow_Publish_from_S3"
            Effect    = "Allow"
            Principal = { Service = "s3.amazonaws.com" }
            Action    = "SNS:Publish"
            Resource  = aws_sns_topic.inbound_received_s3_notifs[each.key].arn
            Condition = {
              ArnLike = {
                "aws:SourceArn" = "${aws_s3_bucket.this.arn}"
              }
            }
          }
        ],
        [
          for index, target in local.eft_partners_config[each.key].inbound.s3_notifications.received_file_targets : {
            Sid       = "Allow_Subscribe_from_${each.key}_${index}"
            Effect    = "Allow"
            Principal = { AWS = target.principal }
            Action    = ["SNS:Subscribe", "SNS:Receive"]
            Resource  = aws_sns_topic.inbound_received_s3_notifs[each.key].arn
            Condition = {
              StringEquals = {
                "sns:Protocol" = target.protocol
              }
              "ForAllValues:StringEquals" = {
                "sns:Endpoint" = [target.arn]
              }
            }
          }
        ]
      )
    }
  )
}

resource "aws_sns_topic" "outbound_pending_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_pending_notifs)

  name              = "${local.full_name}-outbound-pending-s3-${each.key}"
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "outbound_pending_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_pending_notifs)

  arn = aws_sns_topic.outbound_pending_s3_notifs[each.key].arn
  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = concat(
        [
          {
            Sid       = "Allow_Publish_from_S3"
            Effect    = "Allow"
            Principal = { Service = "s3.amazonaws.com" }
            Action    = "SNS:Publish"
            Resource  = aws_sns_topic.outbound_pending_s3_notifs[each.key].arn
            Condition = {
              ArnLike = {
                "aws:SourceArn" = "${aws_s3_bucket.this.arn}"
              }
            }
          }
        ],
        [
          for index, target in local.eft_partners_config[each.key].outbound.s3_notifications.pending_file_targets : {
            Sid       = "Allow_Subscribe_from_${each.key}_${index}"
            Effect    = "Allow"
            Principal = { AWS = target.principal }
            Action    = ["SNS:Subscribe", "SNS:Receive"]
            Resource  = aws_sns_topic.outbound_pending_s3_notifs[each.key].arn
            Condition = {
              StringEquals = {
                "sns:Protocol" = target.protocol
              }
              "ForAllValues:StringEquals" = {
                "sns:Endpoint" = [target.arn]
              }
            }
          }
        ]
      )
    }
  )
}

resource "aws_sns_topic" "outbound_sent_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_sent_notifs)

  name              = "${local.full_name}-outbound-sent-s3-${each.key}"
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "outbound_sent_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_sent_notifs)

  arn = aws_sns_topic.inbound_received_s3_notifs[each.key].arn
  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = concat(
        [
          {
            Sid       = "Allow_Publish_from_S3"
            Effect    = "Allow"
            Principal = { Service = "s3.amazonaws.com" }
            Action    = "SNS:Publish"
            Resource  = aws_sns_topic.outbound_sent_s3_notifs[each.key].arn
            Condition = {
              ArnLike = {
                "aws:SourceArn" = "${aws_s3_bucket.this.arn}"
              }
            }
          }
        ],
        [
          for index, target in local.eft_partners_config[each.key].outbound.s3_notifications.sent_file_targets : {
            Sid       = "Allow_Subscribe_from_${each.key}_${index}"
            Effect    = "Allow"
            Principal = { AWS = target.principal }
            Action    = ["SNS:Subscribe", "SNS:Receive"]
            Resource  = aws_sns_topic.outbound_sent_s3_notifs[each.key].arn
            Condition = {
              StringEquals = {
                "sns:Protocol" = target.protocol
              }
              "ForAllValues:StringEquals" = {
                "sns:Endpoint" = [target.arn]
              }
            }
          }
        ]
      )
    }
  )
}

resource "aws_sns_topic" "outbound_failed_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_failed_notifs)

  name              = "${local.full_name}-outbound-failed-s3-${each.key}"
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "outbound_failed_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_failed_notifs)

  arn = aws_sns_topic.outbound_failed_s3_notifs[each.key].arn
  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = concat(
        [
          {
            Sid       = "Allow_Publish_from_S3"
            Effect    = "Allow"
            Principal = { Service = "s3.amazonaws.com" }
            Action    = "SNS:Publish"
            Resource  = aws_sns_topic.outbound_failed_s3_notifs[each.key].arn
            Condition = {
              ArnLike = {
                "aws:SourceArn" = "${aws_s3_bucket.this.arn}"
              }
            }
          }
        ],
        [
          for index, target in local.eft_partners_config[each.key].outbound.s3_notifications.failed_file_targets : {
            Sid       = "Allow_Subscribe_from_${each.key}_${index}"
            Effect    = "Allow"
            Principal = { AWS = target.principal }
            Action    = ["SNS:Subscribe", "SNS:Receive"]
            Resource  = aws_sns_topic.outbound_failed_s3_notifs[each.key].arn
            Condition = {
              StringEquals = {
                "sns:Protocol" = target.protocol
              }
              "ForAllValues:StringEquals" = {
                "sns:Endpoint" = [target.arn]
              }
            }
          }
        ]
      )
    }
  )
}

resource "aws_s3_bucket_versioning" "this" {
  bucket = aws_s3_bucket.this.id
  versioning_configuration {
    status = "Disabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    id = "${local.full_name}-72hour-object-retention"

    # An empty filter means that this lifecycle applies to _all_ objects within the bucket.
    filter {}

    expiration {
      days = 3 # This bucket has no versioning and so objects will be permanently deleted on expiry
    }

    status = "Enabled"
  }
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

    bucket_key_enabled = true
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
    cidr_blocks     = [data.aws_vpc.this.cidr_block]
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
    target = "/${aws_s3_bucket.this.id}/${local.eft_s3_sftp_home_folder}"
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
  service_name        = data.aws_vpc_endpoint_service.transfer_server.service_name
  subnet_ids          = local.available_endpoint_subnets[*].id
  vpc_endpoint_type   = "Interface"
  vpc_id              = local.vpc_id
  tags                = { Name = "${local.full_name}-sftp-endpoint" }
}
