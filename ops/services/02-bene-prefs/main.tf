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
}

locals {
  service = "bene-prefs"

  default_tags = module.terraservice.default_tags
  env          = module.terraservice.env
  env_key_arn  = module.terraservice.env_key_arn
  name_prefix  = "bfd-${local.env}-${local.service}"
  partners     = toset(["bcda", "ab2d", "dpc"])

  # Reference helper local for future work:
  partner_buckets = {
    for p, m in module.eft_bucket : p => {
      id   = m.bucket.id
      arn  = m.bucket.arn
      name = m.bucket.bucket
    }
  }
}

module "eft_bucket" {
  for_each = local.partners
  source   = "../../terraform-modules/general/secure-bucket"

  bucket_prefix      = "${local.name_prefix}-${each.key}"
  bucket_kms_key_arn = local.env_key_arn
  force_destroy      = false
  ssm_param_name     = "/bfd/${local.env}/${local.service}/${each.value}/nonsensitive/bucket"
  tags               = { Partner = each.value }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  for_each = module.eft_bucket
  bucket   = each.value.bucket.id

  rule {
    id     = "${local.name_prefix}-${each.key}-7day-object-retention"
    status = "Enabled"

    expiration {
      days = 7
    }
  }
}

resource "aws_s3_bucket_notification" "partner_bucket_events" {
  for_each = local.partners
  bucket   = module.eft_bucket[each.key].bucket.id
  topic {
    topic_arn = aws_sns_topic.partner_bucket_events[each.key].arn
    events    = ["s3:ObjectCreated:*"]
  }
  depends_on = [
    aws_sns_topic_policy.partner_bucket_events
  ]
}
