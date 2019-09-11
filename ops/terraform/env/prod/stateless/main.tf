terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.25"
  region = "us-east-1"
}

module "stateless" {
  source = "../../../modules/stateless"

  env_config = {
    env               = "prod"
    tags              = {application="bfd", business="oeda", stack="prod", Environment="prod"}
  }  

  fhir_ami            = var.fhir_ami
  etl_ami             = var.etl_ami
}