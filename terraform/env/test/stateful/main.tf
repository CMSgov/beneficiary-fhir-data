terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.23"
  region = "us-east-1"
}

module "stateful" {
  source = "../../../modules/stateful"

  # Smallish DB
  db_config = { 
    instance_class    = "db.m4.2xlarge"
    iops              = 1000
    allocated_storage = 1000
  }

  env_config = {
    env               = "test"
    tags              = {application="bfd", business="oeda", stack="test", Environment="test"}
  }  
}
