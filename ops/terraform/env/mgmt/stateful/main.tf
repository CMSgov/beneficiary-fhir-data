terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.25"
  region = "us-east-1"
}

module "stateful" {
  source = "../../../modules/mgmt_stateful"

  env_config = {
    env               = "mgmt"
    tags              = {application="bfd", business="oeda", stack="mgmt", Environment="mgmt"}
    azs               = "us-east-1a"
  }
}
