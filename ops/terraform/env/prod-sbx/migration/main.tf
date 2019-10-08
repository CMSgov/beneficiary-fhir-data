terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.25"
  region = "us-east-1"
}

module "migration" {
  source = "../../../modules/migration"

  env_config = {
    env               = "prod-sbx"
    tags              = {application="bfd", business="oeda", stack="prod-sbx", Environment="prod-sbx"}
  }  

  bb      = 50
  bcda    = 100
  dpc     = 100
  mct     = 100
}
