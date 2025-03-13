locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
  env        = "mgmt"
  config_cmk = data.aws_kms_key.config_cmk
  cloudtamer_iam_path = "/delegatedadmin/developer/"
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
data "aws_kms_key" "config_cmk" {
  key_id = "alias/bfd-mgmt-config-cmk"
}
