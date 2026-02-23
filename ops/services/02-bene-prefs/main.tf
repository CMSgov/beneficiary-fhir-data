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
  service        = "beneficiary-prefs"
  target_service = "beneprefs"

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
  azs                      = keys(module.terraservice.default_azs)

  name_prefix = "bfd-${local.env}-${local.service}"
  partners    = toset(["bcda", "ab2d", "dpc"])
}

# Create the buckets
module "eft_bucket" {
  for_each = local.partners
  source   = "../../terraform-modules/general/secure-bucket"

  bucket_prefix      = "${local.name_prefix}-${each.key}"
  bucket_kms_key_arn = local.env_key_arn
  force_destroy      = false

  tags = {
    Partner = each.value
  }

  ssm_param_name = "/bfd/${local.env}/${local.service}/${each.value}/nonsensitive/bucket"

}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  for_each = module.eft_bucket
  bucket   = each.value.bucket.id

  rule {
    id = "${local.name_prefix}-${each.key}-7day-object-retention"
    filter {}
    status = "Enabled"

    expiration {
      days = 7
    }
  }
}
