locals {
  account_id = data.aws_caller_identity.current.account_id
  env        = "mgmt"
  kms_key_id = data.aws_kms_key.cmk.arn

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

