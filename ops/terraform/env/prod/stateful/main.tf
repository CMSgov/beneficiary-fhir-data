terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.25"
  region = "us-east-1"
}

module "stateful" {
  source = "../../../modules/stateful"

  # Large DB
  db_config = { 
    instance_class    = "db.m5.4xlarge"
    iops              = 8000
    allocated_storage = 12000
  }

  env_config = {
    env               = "prod"
    tags              = {application="bfd", business="oeda", stack="prod", Environment="prod"}
  }  
}
