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
  latest_version   = module.terraservice.latest_bfd_release

  service   = "eft"
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  inbound_eft_partners  = jsondecode(nonsensitive(data.aws_ssm_parameter.partners_list_json["inbound"].value))
  outbound_eft_partners = jsondecode(nonsensitive(data.aws_ssm_parameter.partners_list_json["outbound"].value))
  eft_partners          = distinct(concat(local.inbound_eft_partners, local.outbound_eft_partners))
  ssm_hierarchy_roots   = concat(["bfd"], local.eft_partners)
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
  # FUTURE: Refactor something like this out into a distinct module much like bfd-terraservice above
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
    local.ssm_config["/bfd/${local.service}/inbound/sftp_server/subnet_to_ip_reservations_nlb_json"]
  )
  inbound_sftp_server_key    = local.ssm_config["/bfd/${local.service}/inbound/sftp_server/host_private_key"]
  inbound_r53_hosted_zone    = local.ssm_config["/bfd/${local.service}/inbound/sftp_server/r53_hosted_zone"]
  inbound_sftp_user_pub_key  = local.ssm_config["/bfd/${local.service}/inbound/sftp_server/eft_user_public_key"]
  inbound_sftp_user_username = local.ssm_config["/bfd/${local.service}/inbound/sftp_server/eft_user_username"]
  inbound_sftp_s3_home_dir   = trim(local.ssm_config["/bfd/${local.service}/inbound/sftp_server/eft_user_home_dir"], "/")
  # Global Inbound configuration is required, as the SFTP server should always be running and the
  # home directory is global, but this outbound configuration is not required. If any are undefined,
  # outbound is considered to be disabled globally for all partners and corresponding resources will
  # not be created.
  outbound_sftp_host          = lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/host", null)
  outbound_sftp_host_key      = lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/trusted_host_key", null)
  outbound_sftp_username      = lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/username", null)
  outbound_sftp_user_priv_key = lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/user_priv_key", null)
  # First, construct the configuration for each partner. Partners with invalid path configuration
  # will be discarded below. We could assume that configuration is infallible for all properties, or
  # that invaild values will fail fast. But, invalid paths may not cause Terraform (really, AWS) to
  # fail fast when generating corresponding infrastructure. We don't want that to happen, so we need
  # to check those preconditions manually. The verbosity and repetition is intentional, albeit
  # unfortunate, as Terraform does not support the language constructs necessary to reduce it
  unfiltered_eft_partners_config = {
    for partner in local.eft_partners :
    partner => {
      bucket_home_path = join(
        "/",
        [
          # Terraform doesn't support functions, so this pattern is duplicated wherever a path is
          # constructed from config. This guards against empty, null, or whitespace-only paths,
          # taking advantage of coalesce to do a "null or empty" check all at once and replace to
          # strip out whitespace such that the full check becomes "is not null or whitespace"
          for path in [
            local.inbound_sftp_s3_home_dir,
            trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/")
          ] : path if coalesce(replace(path, "/\\s/", ""), "INVALID") != "INVALID"
        ]
      ),
      bucket_iam_assumer_arns = jsondecode(
        lookup(local.ssm_config, "/${partner}/${local.service}/bucket_iam_assumer_arns_json", "[]")
      )
      inbound = {
        dir = join(
          "/",
          [
            for path in [
              local.inbound_sftp_s3_home_dir,
              trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
              trim(local.ssm_config["/${partner}/${local.service}/inbound/dir"], "/")
            ] : path if coalesce(replace(path, "/\\s/", ""), "INVALID") != "INVALID"
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
            for path in [
              local.inbound_sftp_s3_home_dir,
              trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
              trim(local.ssm_config["/${partner}/${local.service}/outbound/pending_dir"], "/")
            ] : path if coalesce(replace(path, "/\\s/", ""), "INVALID") != "INVALID"
          ]
        ),
        sent_path = join(
          "/",
          [
            for path in [
              local.inbound_sftp_s3_home_dir,
              trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
              trim(local.ssm_config["/${partner}/${local.service}/outbound/sent_dir"], "/")
            ] : path if coalesce(replace(path, "/\\s/", ""), "INVALID") != "INVALID"
          ]
        ),
        failed_path = join(
          "/",
          [
            for path in [
              local.inbound_sftp_s3_home_dir,
              trim(local.ssm_config["/${partner}/${local.service}/bucket_home_dir"], "/"),
              trim(local.ssm_config["/${partner}/${local.service}/outbound/failed_dir"], "/")
            ] : path if coalesce(replace(path, "/\\s/", ""), "INVALID") != "INVALID"
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
  # Filter out any partners with invalid path configuration (such as paths with whitespace in them,
  # or if any of the paths for a given partner are null or empty). Additionally, a partner is
  # considered invalid if they have no configured Trust Relationships, as otherwise they are unable
  # to interact with the BFD EFT S3 Bucket and therefore unable to use BFD EFT in any capacity
  eft_partners_config = {
    for partner, data in local.unfiltered_eft_partners_config :
    partner => data if alltrue([
      for path in flatten([
        [local.unfiltered_eft_partners_config[partner].bucket_home_path],
        contains(local.inbound_eft_partners, partner) ? [local.unfiltered_eft_partners_config[partner].inbound.dir] : [],
        contains(local.outbound_eft_partners, partner) ? [
          local.unfiltered_eft_partners_config[partner].outbound.pending_path,
          local.unfiltered_eft_partners_config[partner].outbound.sent_path,
          local.unfiltered_eft_partners_config[partner].outbound.failed_path
        ] : []
      ]) :
      coalesce(path, "INVALID") != "INVALID" && length(regexall("\\s", path)) == 0
    ]) && length(local.unfiltered_eft_partners_config[partner].bucket_iam_assumer_arns) > 0
  }
  # List of _valid_ partners (after the above filtering) that have been configured with inbound
  # enabled.
  eft_partners_with_inbound_enabled = [
    for partner, _ in local.eft_partners_config :
    partner if contains(local.inbound_eft_partners, partner)
  ]
  # List of _valid_ partners (after the above filtering) that have been configured with outbound
  # enabled. Outbound will be globally disabled (this will be an empty list) if any global outbound
  # configuration is undefined or invalid. Additionally, partners must have at least one recognized
  # file configured in order for outbound to be considered enabled for them
  eft_partners_with_outbound_enabled = length(
    # Essentially, this checks every global outbound configuration value to ensure none of them are
    # null or whitespace. If any are, the ternary will return an empty list of outbound partners, as
    # outbound requires this configuration to be defined
    compact([
      for x in [
        local.outbound_sftp_host,
        local.outbound_sftp_host_key,
        local.outbound_sftp_username,
        local.outbound_sftp_user_priv_key
      ] : trimspace(x)
    ])
    ) > 0 ? [
    for partner, _ in local.eft_partners_config :
    partner
    if contains(local.outbound_eft_partners, partner) && length(
      jsondecode(
        lookup(local.ssm_config, "/${partner}/${local.service}/outbound/s3_notifications/recognized_files_json", "[]")
      )
    ) > 0
  ] : []
  eft_partners_with_inbound_received_notifs = [
    for partner in local.eft_partners_with_inbound_enabled :
    partner
    if length(local.eft_partners_config[partner].inbound.s3_notifications.received_file_targets) > 0
  ]
  eft_partners_with_outbound_sent_notifs = [
    for partner in local.eft_partners_with_outbound_enabled :
    partner
    if length(local.eft_partners_config[partner].outbound.s3_notifications.sent_file_targets) > 0
  ]
  eft_partners_with_outbound_failed_notifs = [
    for partner in local.eft_partners_with_outbound_enabled :
    partner
    if length(local.eft_partners_config[partner].outbound.s3_notifications.failed_file_targets) > 0
  ]

  account_id     = data.aws_caller_identity.current.account_id
  region         = data.aws_region.current.name
  vpc_id         = data.aws_vpc.this.id
  kms_key_id     = data.aws_kms_key.cmk.arn
  sftp_port      = 22
  logging_bucket = "bfd-${local.seed_env}-logs-${local.account_id}"

  outbound_lambda_name      = "sftp-outbound-transfer"
  outbound_lambda_full_name = "${local.full_name}-${local.outbound_lambda_name}"
  outbound_lambda_src       = replace(local.outbound_lambda_name, "-", "_")
  outbound_lambda_image_uri = "${data.aws_ecr_repository.ecr.repository_url}:${local.latest_version}"
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
    local.eft_partners_with_outbound_enabled,
    local.eft_partners_with_outbound_sent_notifs,
    local.eft_partners_with_outbound_failed_notifs
  ])) > 0 ? 1 : 0

  bucket = aws_s3_bucket.this.id

  dynamic "topic" {
    for_each = toset(local.eft_partners_with_inbound_received_notifs)
    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.key].inbound.dir}/"
      id            = aws_sns_topic.inbound_received_s3_notifs[topic.key].name
      topic_arn     = aws_sns_topic.inbound_received_s3_notifs[topic.key].arn
    }
  }

  dynamic "topic" {
    # Unlike other outbound notification classes, we want to enable S3 Event Notifications for files
    # landing in a partner's pending directory even if they have no configured targets if outbound
    # is enabled for them (specifically, if they have any recognized files configured). This is
    # necessary to invoke the outbound SFTP Lambda
    for_each = toset(local.eft_partners_with_outbound_enabled)
    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.key].outbound.pending_path}/"
      id            = aws_sns_topic.outbound_pending_s3_notifs[topic.key].name
      topic_arn     = aws_sns_topic.outbound_pending_s3_notifs[topic.key].arn
    }
  }

  dynamic "topic" {
    for_each = toset(local.eft_partners_with_outbound_sent_notifs)
    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.key].outbound.sent_path}/"
      id            = aws_sns_topic.outbound_sent_s3_notifs[topic.key].name
      topic_arn     = aws_sns_topic.outbound_sent_s3_notifs[topic.key].arn
    }
  }

  dynamic "topic" {
    for_each = toset(local.eft_partners_with_outbound_failed_notifs)
    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.key].outbound.failed_path}/"
      id            = aws_sns_topic.outbound_failed_s3_notifs[topic.key].name
      topic_arn     = aws_sns_topic.outbound_failed_s3_notifs[topic.key].arn
    }
  }
}

resource "aws_lambda_function" "sftp_outbound_transfer" {
  count = length(local.eft_partners_with_outbound_enabled) > 0 ? 1 : 0

  function_name = local.outbound_lambda_full_name

  description = join("", [
    "Invoked when participating Peering Partners upload files to their corresponding outbound ",
    "pending files folder in the ${local.full_name} Bucket. This Lambda will then upload those ",
    "file(s) via CMS EFT SFTP if they are recognized and valid."
  ])

  kms_key_arn  = local.kms_key_id
  image_uri    = local.outbound_lambda_image_uri
  package_type = "Image"
  memory_size  = 512
  timeout      = 600

  tags = {
    Name = local.outbound_lambda_full_name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT = local.env
      BUCKET          = local.full_name
      BUCKET_ROOT_DIR = local.inbound_sftp_s3_home_dir
    }
  }

  role = one(aws_iam_role.sftp_outbound_transfer[*].arn)

  # FUTURE: Implement VPC config allowing Lambda to interact _only_ with the EFT SFTP Server
}

resource "aws_lambda_permission" "sftp_outbound_transfer_sns" {
  for_each = toset(local.eft_partners_with_outbound_enabled)

  statement_id   = "${local.outbound_lambda_full_name}-allow-sns"
  action         = "lambda:InvokeFunction"
  function_name  = one(aws_lambda_function.sftp_outbound_transfer[*].function_name)
  principal      = "sns.amazonaws.com"
  source_arn     = aws_sns_topic.outbound_pending_s3_notifs[each.key].arn
  source_account = local.account_id
}

resource "aws_sns_topic_subscription" "sftp_outbound_transfer" {
  for_each = toset(local.eft_partners_with_outbound_enabled)

  topic_arn = aws_sns_topic.outbound_pending_s3_notifs[each.key].arn
  protocol  = "lambda"
  endpoint  = one(aws_lambda_function.sftp_outbound_transfer[*].arn)
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
  for_each = toset(local.eft_partners_with_outbound_enabled)

  name              = "${local.full_name}-outbound-pending-s3-${each.key}"
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "outbound_pending_s3_notifs" {
  for_each = toset(local.eft_partners_with_outbound_enabled)

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

  arn = aws_sns_topic.outbound_sent_s3_notifs[each.key].arn
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
  host_key               = local.inbound_sftp_server_key
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
  user_name = local.inbound_sftp_user_username
  tags      = { Name = "${local.full_name}-sftp-user-${local.inbound_sftp_user_username}" }

  home_directory_type = "LOGICAL"

  home_directory_mappings {
    entry  = "/"
    target = "/${aws_s3_bucket.this.id}/${local.inbound_sftp_s3_home_dir}"
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
