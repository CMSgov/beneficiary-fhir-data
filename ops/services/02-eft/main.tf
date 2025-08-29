terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = true
  service              = local.service
  relative_module_root = "ops/services/02-eft"
  subnet_layers        = ["private"]
  ssm_hierarchy_roots  = local.ssm_hierarchy_roots
}

locals {
  service = "eft"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = nonsensitive(module.terraservice.ssm_config)
  env_key_alias            = module.terraservice.env_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  subnets                  = module.terraservice.subnets_map["private"]
  azs                      = keys(module.terraservice.default_azs)

  full_name = "bfd-${local.env}-${local.service}"

  inbound_eft_partners  = jsondecode(nonsensitive(data.aws_ssm_parameter.partners_list_json["inbound"].value))
  outbound_eft_partners = jsondecode(nonsensitive(data.aws_ssm_parameter.partners_list_json["outbound"].value))
  eft_partners          = distinct(concat(local.inbound_eft_partners, local.outbound_eft_partners))
  ssm_hierarchy_roots   = concat(["bfd"], local.eft_partners)

  inbound_sftp_server_key    = sensitive(local.ssm_config["/bfd/${local.service}/inbound/sftp_server/host_private_key"])
  inbound_sftp_user_pub_key  = local.ssm_config["/bfd/${local.service}/inbound/sftp_server/eft_user_public_key"]
  inbound_sftp_user_username = local.ssm_config["/bfd/${local.service}/inbound/sftp_server/eft_user_username"]
  inbound_sftp_s3_home_dir   = trim(local.ssm_config["/bfd/${local.service}/inbound/sftp_server/eft_user_home_dir"], "/")
  # Special case for BCDA. ISP does not use the typical BFD EFT Inbound process, they instead upload
  # directly to the BCDA inbound path. These configuration parameters are used to create an IAM Role
  # that ISP assumes to do the aforementioned upload.
  bcda_isp_bucket_assumer_arns = jsondecode(lookup(local.ssm_config, "/bcda/${local.service}/inbound/isp_bucket_iam_assumer_arns_json", "[]"))
  bcda_isp_vpc_endpoint_id     = lookup(local.ssm_config, "/bcda/${local.service}/inbound/isp_vpc_endpoint_id", null)
  # Global Inbound configuration is required, as the SFTP server should always be running and the
  # home directory is global, but this outbound configuration is not required. If any are undefined,
  # outbound is considered to be disabled globally for all partners and corresponding resources will
  # not be created.
  outbound_sftp_host              = lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/host", null)
  outbound_sftp_trusted_host_keys = lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/trusted_host_keys_json", null)
  outbound_sftp_username          = lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/username", null)
  outbound_sftp_user_priv_key     = sensitive(lookup(local.ssm_config, "/bfd/${local.service}/outbound/sftp/user_priv_key", null))
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
              trim(lookup(local.ssm_config, "/${partner}/${local.service}/bucket_home_dir", ""), "/"),
              trim(lookup(local.ssm_config, "/${partner}/${local.service}/inbound/dir", ""), "/")
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
              trim(lookup(local.ssm_config, "/${partner}/${local.service}/bucket_home_dir", ""), "/"),
              trim(lookup(local.ssm_config, "/${partner}/${local.service}/outbound/pending_dir", ""), "/")
            ] : path if coalesce(replace(path, "/\\s/", ""), "INVALID") != "INVALID"
          ]
        ),
        notification_targets = jsondecode(
          lookup(local.ssm_config, "/${partner}/${local.service}/outbound/notification_targets_json", "[]")
        ),
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
        contains(local.inbound_eft_partners, partner) ? [
          local.unfiltered_eft_partners_config[partner].inbound.dir
        ] : [],
        contains(local.outbound_eft_partners, partner) ? [
          local.unfiltered_eft_partners_config[partner].outbound.pending_path
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
  eft_partners_with_outbound_enabled = alltrue([
    # Essentially, this checks every global outbound configuration value to ensure none of them are
    # null or whitespace. If any are, the ternary will return an empty list of outbound partners, as
    # outbound requires this configuration to be defined
    for x in [
      local.outbound_sftp_host,
      local.outbound_sftp_trusted_host_keys,
      local.outbound_sftp_username,
      nonsensitive(local.outbound_sftp_user_priv_key)
    ] : trimspace(coalesce(x, "INVALID")) != "INVALID"]) ? [
    for partner, _ in local.eft_partners_config :
    partner
    if contains(local.outbound_eft_partners, partner) && length(
      jsondecode(
        lookup(local.ssm_config, "/${partner}/${local.service}/outbound/recognized_files_json", "[]")
      )
    ) > 0
  ] : []
  eft_partners_with_inbound_received_notifs = [
    for partner in local.eft_partners_with_inbound_enabled :
    partner
    if length(local.eft_partners_config[partner].inbound.s3_notifications.received_file_targets) > 0
  ]
  eft_partners_with_outbound_notifs = [
    for partner in local.eft_partners_with_outbound_enabled :
    partner
    if length(local.eft_partners_config[partner].outbound.notification_targets) > 0
  ]

  vpc_id = local.vpc.id

  sftp_port      = 22
  sftp_full_name = "${local.full_name}-sftp"

  topic_arn_placeholder = "%TOPIC_ARN%"

  outbound_notifs_topic_prefix = "${local.full_name}-outbound-events"
  # For some reason, the transfer server endpoint service does not support us-east-1b and instead
  # opts to support us-east-1d. In order to enable support for this sub-az in the future
  # automatically (if transfer server VPC endpoints begin to support 1c), we filter our desired
  # subnets against the supported availability zones taking only those that belong to supported azs
  available_endpoint_azs = setintersection(
    data.aws_vpc_endpoint_service.transfer_server.availability_zones,
    local.subnets[*].availability_zone
  )
  available_endpoint_subnets = [
    for subnet in local.subnets
    : subnet if contains(local.available_endpoint_azs, subnet.availability_zone)
  ]
}

data "aws_iam_policy_document" "isp_bcda" {
  count = length(aws_iam_role.isp_bcda_bucket_access) > 0 ? 1 : 0

  statement {
    sid    = "AllowISPFromVPCEOnly"
    effect = "Deny"

    actions = ["s3:*"]
    resources = [
      module.bucket_eft.bucket.arn,
      "${module.bucket_eft.bucket.arn}/*"
    ]

    principals {
      type        = "AWS"
      identifiers = aws_iam_role.isp_bcda_bucket_access[*].arn
    }
    condition {
      test     = "StringNotEquals"
      variable = "aws:SourceVpce"
      values   = [local.bcda_isp_vpc_endpoint_id]
    }
  }
}

module "bucket_eft" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_prefix                 = local.full_name
  bucket_kms_key_arn            = local.env_key_arn
  force_destroy                 = false
  additional_bucket_policy_docs = data.aws_iam_policy_document.isp_bcda[*].json

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/bucket"
}

resource "aws_s3_bucket_versioning" "this" {
  bucket = module.bucket_eft.bucket.id
  versioning_configuration {
    status = "Disabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  bucket = module.bucket_eft.bucket.id

  rule {
    id = "${local.full_name}-7day-object-retention"

    # An empty filter means that this lifecycle applies to _all_ objects within the bucket.
    filter {}

    expiration {
      days = 7 # This bucket has no versioning and so objects will be permanently deleted on expiry
    }

    status = "Enabled"
  }
}

resource "aws_s3_bucket_notification" "bucket_notifications" {
  count = length(concat(
    local.eft_partners_with_inbound_received_notifs,
    local.eft_partners_with_outbound_enabled,
  )) > 0 ? 1 : 0

  bucket = module.bucket_eft.bucket.id

  dynamic "topic" {
    for_each = toset(local.eft_partners_with_inbound_received_notifs)
    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.key].inbound.dir}/"
      id            = module.topic_inbound_received_s3_notifs[topic.key].topic.name
      topic_arn     = module.topic_inbound_received_s3_notifs[topic.key].topic.arn
    }
  }

  dynamic "topic" {
    for_each = toset(local.eft_partners_with_outbound_enabled)
    content {
      events        = ["s3:ObjectCreated:*"]
      filter_prefix = "${local.eft_partners_config[topic.key].outbound.pending_path}/"
      id            = module.topic_outbound_pending_s3_notifs[topic.key].topic.name
      topic_arn     = module.topic_outbound_pending_s3_notifs[topic.key].topic.arn
    }
  }
}
