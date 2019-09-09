terraform {
  required_version = "<=0.12.5"
}

provider "aws" {
  version = "~> 2.25"
  region = "us-east-1"
}

module "stateless" {
  source = "../../../modules/stateless"

  env_config = {
    env               = "test"
    tags              = {application="bfd", business="oeda", stack="test", Environment="test"}
  }  

  fhir_ami            = var.fhir_ami
  etl_ami             = var.etl_ami
  key_name            = var.ssh_key_name
}
