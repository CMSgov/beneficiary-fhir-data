terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-platform-service"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/platform/02-alerting"
}

locals {
  service = "alerting"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  kms_key_alias            = module.terraservice.key_alias
  kms_key_arn              = module.terraservice.key_arn
  ssm_config               = module.terraservice.ssm_config
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn

  name_prefix = "bfd-platform"

  slack_channels = jsondecode(nonsensitive(local.ssm_config["/bfd/alerting/slack/channels_list_json"]))
}

data "aws_iam_policy_document" "service_assume_role" {
  for_each = toset(["sns", "lambda"])
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["${each.value}.amazonaws.com"]
    }
  }
}
