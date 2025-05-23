module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/000-bootstrap"
  lookup_kms_keys      = false
}

locals {
  service = "bootstrap"

  region       = module.terraservice.region
  account_id   = module.terraservice.account_id
  default_tags = module.terraservice.default_tags

  state_variants = local.account_type == "non-prod" ? ["test", "platform-non-prod"] : ["prod-sbx", "prod", "platform-prod"]
}
