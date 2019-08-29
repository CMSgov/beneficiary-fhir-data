// AWS SSM Parameter Store

locals {
  tags                    = merge({Layer="data", role=var.role}, var.env_config.tags)
  is_prod                 = substr(var.env_config.env, 0, 4) == "prod" 
}

data "aws_caller_identity" "current" {}

resource "aws_kms_key" "bfd_ssm_kms_key" {
  description             = "BFD KMS Key for SSM Parameter Store"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}

resource "aws_kms_alias" "bfd_ssm_kms_alias" {
  name          = "alias/bfd-ssm-kms"
  target_key_id = aws_kms_key.hub_ssm_kms_key.key_id
}