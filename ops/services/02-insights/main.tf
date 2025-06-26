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

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/02-insights"
}

locals {
  service  = "insights"
  partners = ["bcda", "bfd", "bb2", "ab2d"]

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  latest_bfd_release       = module.terraservice.latest_bfd_release
  ssm_config               = module.terraservice.ssm_config
  env_key_alias            = module.terraservice.env_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  azs                      = keys(module.terraservice.default_azs)
}

# Temporary placeholder for Greenfield Insights Bucket structure to enable applying server-insights.
# Legacy has a single Bucket, but Greenfield will have a bucket per-environment, per-consumer
# TODO: Make this Terraservice more useful
module "bucket_insights_bfd" {
  for_each = var.greenfield ? toset(local.partners) : toset([])
  source   = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_prefix      = "bfd-${local.env}-insights-${each.key}"
  force_destroy      = local.is_ephemeral_env

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/bfd-insights-${each.key}"
}

#This bucket is currently being used by all partners in their respective Insights resources
module "bucket_insights_moderate" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_prefix      = "bfd-${local.env}-insights-moderate"
  force_destroy      = local.is_ephemeral_env

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/bfd-insights-moderate"
}

resource "aws_s3_object" "bfd_bucket_prefixes" {
  for_each = var.greenfield ? merge(
    {
      for partner in local.partners : "${partner}-workgroups" => {
        partner = partner
        prefix  = "workgroups/"
      }
    },
    {
      for partner in local.partners : "${partner}-databases" => {
        partner = partner
        prefix  = "databases/"
      }
    }
  ) : {}

  bucket       = module.bucket_insights_bfd[each.value.partner].bucket.bucket
  key          = each.value.prefix
  content      = ""
  content_type = "application/octet-stream"
}

resource "aws_athena_workgroup" "this" {
  for_each = var.greenfield ? toset(local.partners) : toset([])
  name     = "${local.env}-${each.key}"

  configuration {
    enforce_workgroup_configuration    = true
    publish_cloudwatch_metrics_enabled = true

    result_configuration {
      output_location = "s3://${module.bucket_insights_bfd[each.key].bucket.bucket}/results/"

      encryption_configuration {
        encryption_option = "SSE_KMS"
        kms_key_arn       = module.terraservice.env_key_arn
      }
    }
  }

  state = "ENABLED"
}
