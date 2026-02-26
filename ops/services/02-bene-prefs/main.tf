terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  service              = local.service
  relative_module_root = "ops/services/02-bene-prefs"
  ssm_hierarchy_roots   = concat(["bfd"], tolist(local.partners))
}

locals {
  service = "bene-prefs"

  ssm_config               = nonsensitive(module.terraservice.ssm_config)
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  env_key_arn              = module.terraservice.env_key_arn
  name_prefix              = "bfd-${local.env}-${local.service}"
  partners                 = toset(["bcda", "ab2d", "dpc"])
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn

  root_dir = "bfdeft01"

  # First, construct the configuration for each partner. Partners with invalid path configuration
  # will be discarded below. We could assume that configuration is infallible for all properties, or
  # that invaild values will fail fast. But, invalid paths may not cause Terraform (really, AWS) to
  # fail fast when generating corresponding infrastructure. We don't want that to happen, so we need
  # to check those preconditions manually. The verbosity and repetition is intentional, albeit
  # unfortunate, as Terraform does not support the language constructs necessary to reduce it
  partners_config = {
    for partner in local.partners :
    partner => {
      bucket_home_path = "/${local.root_dir}/${partner}/in",
      bucket_iam_assumer_arns = jsondecode(
        lookup(local.ssm_config, "/${partner}/${local.service}/bucket_iam_assumer_arns_json", "[]")
      )
      s3_notifications = {
        received_file_targets = jsondecode(
          lookup(local.ssm_config, "/${partner}/${local.service}/inbound/s3_notifications/received_file_targets_json", "[]")
        )
      }
    }
  }

  topic_arn_placeholder = "%TOPIC_ARN%"
}

module "buckets" {
  for_each = local.partners
  source   = "../../terraform-modules/general/secure-bucket"

  bucket_prefix      = "${local.name_prefix}-${each.key}"
  bucket_kms_key_arn = local.env_key_arn
  force_destroy      = false
  ssm_param_name     = "/bfd/${local.env}/${local.service}/${each.value}/nonsensitive/bucket"
  tags               = { Partner = each.value }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  for_each = module.buckets
  bucket   = each.value.bucket.id

  rule {
    id     = "${local.name_prefix}-${each.key}-7day-object-retention"
    status = "Enabled"

    expiration {
      days = 7
    }
  }
}

resource "aws_s3_bucket_notification" "buckets" {
  for_each = local.partners
  bucket   = module.buckets[each.key].bucket.id
  topic {
    topic_arn = module.topics[each.key].topic.arn
    events    = ["s3:ObjectCreated:*"]
  }
}

data "aws_iam_policy_document" "topic_received_s3_notifs" {
  for_each = toset(local.partners)

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
      values   = ["${module.buckets[each.key].bucket.arn}"]
    }
  }

  dynamic "statement" {
    for_each = {
      for index, target in local.partners_config[each.key].s3_notifications.received_file_targets
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

module "topics" {
  for_each = toset(local.partners)

  source = "../../terraform-modules/general/logging-sns-topic"

  topic_name                   = "${local.name_prefix}-received-s3-${each.key}"
  additional_topic_policy_docs = [data.aws_iam_policy_document.topic_received_s3_notifs[each.key].json]

  iam_path                 = local.iam_path
  permissions_boundary_arn = local.permissions_boundary_arn

  kms_key_arn = local.env_key_arn

  sqs_sample_rate    = 100
  lambda_sample_rate = 100
}
