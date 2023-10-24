locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
  env        = "mgmt"
  mgmt_cmk   = data.aws_kms_key.mgmt
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
data "aws_kms_key" "mgmt" {
  key_id = "alias/bfd-mgmt-cmk"
}
