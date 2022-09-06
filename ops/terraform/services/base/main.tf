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
  env              = terraform.workspace
  is_ephemeral_env = !(contains(local.established_envs, local.env))

  established_envs = [
    "test",
    "mgmt",
    "prod-sbx",
    "prod"
  ]
}

data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}
