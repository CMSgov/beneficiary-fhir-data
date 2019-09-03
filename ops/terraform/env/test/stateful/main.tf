terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.25"
  region = "us-east-1"
}

module "stateful" {
  source = "../../../modules/stateful"

  db_config = { 
    instance_class    = "db.m5.4xlarge"
    iops              = 16000
    allocated_storage = 8000
  }

  db_import_mode = {
    enabled = true
    maintenance_work_mem = "2097152"
  }

  env_config = {
    env               = "test"
    tags              = {application="bfd", business="oeda", stack="test", Environment="test"}
  }  
}
