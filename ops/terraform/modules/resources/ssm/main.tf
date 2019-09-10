// AWS SSM Parameter Store

locals {
  tags                    = merge({Layer="data", role=var.role}, var.env_config.tags)
  is_prod                 = substr(var.env_config.env, 0, 4) == "prod" 
}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "master_key" {
  description = "bfd-${var.env_config.env}-master-key"
  tags        = var.env_config.tags
}

data "aws_kms_alias" "app-config-key-alias" {
  name          = "alias/bfd-${var.env_config.env}-master-key"
  target_key_id = aws_kms_key.master_key.key_id
}