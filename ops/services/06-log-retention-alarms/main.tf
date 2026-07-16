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
  relative_module_root = "ops/services/06-log-retention-alarms"
}

locals {
  service = "log-retention-alarms"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = module.terraservice.ssm_config
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn

  name_prefix = "bfd-${local.env}-${local.service}"

  checker_lambda_name      = "log-retention-checker"
  checker_lambda_abbv_name = "${local.name_prefix}-log-ret-check"
  checker_lambda_full_name = "${local.name_prefix}-${local.checker_lambda_name}"
  checker_lambda_src       = replace(local.checker_lambda_name, "-", "_")

  checker_lambda_rate        = nonsensitive(lookup(local.ssm_config, "/bfd/${local.service}/checker/rate", "10 minutes"))
  checker_alert_topic_name   = nonsensitive(lookup(local.ssm_config, "/bfd/${local.service}/sns_topics/logs/alert", null))
  required_retention_in_days = nonsensitive(lookup(local.ssm_config, "/bfd/${local.service}/checker/required_retention_in_days", "2557"))
}