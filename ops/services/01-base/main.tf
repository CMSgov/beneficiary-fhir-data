module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = var.greenfield
  parent_env           = local.parent_env
  environment_name     = terraform.workspace
  service              = local.service
  relative_module_root = "ops/services/01-base"
}

locals {
  service = "base"

  region             = module.terraservice.region
  account_id         = module.terraservice.account_id
  default_tags       = module.terraservice.default_tags
  env                = module.terraservice.env
  is_ephemeral_env   = module.terraservice.is_ephemeral_env
  latest_bfd_release = module.terraservice.latest_bfd_release

  dr_region = data.aws_region.secondary.name

  kms_default_deletion_window_days = 30
}

data "aws_region" "secondary" {
  provider = aws.secondary
}

# Define a default key policy doc that allows our root account and admins to manage and delegate access to the key. This
# policy statement must be included in every kms key policy, whether you use this doc or not. Without it, our account
# and account admins will not be able to manage the key. See the following for more info on default key policies:
#  - https://docs.aws.amazon.com/kms/latest/developerguide/key-policies.html#key-policy-default
data "aws_iam_policy_document" "default_kms_key_policy" {
  statement {
    sid    = "AllowKeyManagement"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${local.account_id}:root"]
    }
    actions   = ["kms:*"]
    resources = ["*"]
  }
}

