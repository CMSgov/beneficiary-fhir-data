locals {
  env = "test"
  default_tags = {
    application    = "bfd"
    business       = "oeda"
    stack          = local.env
    Environment    = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/env/${local.env}/stateful"
  }
}

module "stateful" {
  source = "../../../modules/stateful"
}
