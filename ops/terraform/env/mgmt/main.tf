locals {
  account_id          = data.aws_caller_identity.current.account_id
  env                 = "mgmt"
  kms_key_id          = data.aws_kms_key.cmk.arn
  test_kms_key_id     = data.aws_kms_key.test_cmk.arn
  prod_sbx_kms_key_id = data.aws_kms_key.prod_sbx_cmk.arn
  prod_kms_key_id     = data.aws_kms_key.prod_cmk.arn

  shared_tags = {
    Environment = local.env
    application = "bfd"
    business    = "oeda"
    stack       = local.env
  }
}

data "aws_caller_identity" "current" {}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}

data "aws_kms_key" "test_cmk" {
  key_id = "alias/bfd-test-cmk"
}

data "aws_kms_key" "prod_sbx_cmk" {
  key_id = "alias/bfd-prod-sbx-cmk"
}

data "aws_kms_key" "prod_cmk" {
  key_id = "alias/bfd-prod-cmk"
}

# TODO: As of late July 2022, this is parameter is manually managed.
data "aws_ssm_parameter" "cbc_aws_account_arn" {
  name            = "/bfd/mgmt/jenkins/sensitive/cbc_aws_account_arn"
  with_decryption = true
}
