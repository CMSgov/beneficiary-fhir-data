locals {
  service = "backup"

  region       = module.terraservice.region
  account_id   = module.terraservice.account_id
  default_tags = module.terraservice.default_tags
  kms_key_arn  = module.terraservice.key_arn
  ssm_config   = module.terraservice.ssm_config
}
