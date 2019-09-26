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
    env               = "test"
    tags              = {application="bfd", business="oeda", stack="test", Environment="test"}
  }  

  bb      = 100
  bcda    = 100
  dpc     = 100
  mct     = 100
}
