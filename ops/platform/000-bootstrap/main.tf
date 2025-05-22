locals {
  service = "bootstrap"

  default_tags = {
    application    = "bfd"
    business       = "oeda"
    service        = "bootstrap"
    Terraform      = true
    tf_module_root = "ops/platform/000-bootstrap"
  }

  account_id     = data.aws_caller_identity.current.account_id
  region         = data.aws_region.current.name
  account_alias  = data.aws_iam_account_alias.current.account_alias
  state_variants = strcontains(local.account_alias, "non-prod") ? ["test", "platform-non-prod"] : ["prod-sbx", "prod", "platform-prod"]
}

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

data "aws_iam_account_alias" "current" {}
