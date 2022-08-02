provider "aws" {
  region = "us-east-1"
  default_tags {
    tags = {
      Environment = local.env
      application = "bfd"
      business    = "oeda"
      stack       = local.env
    }
  }
}

locals {
  env = terraform.workspace
  established_envs = [
    "test",
    "mgmt",
    "prod-sbx",
    "prod"
  ]

  kms_key_alias = "alias/bfd-${local.env}-cmk"
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}
