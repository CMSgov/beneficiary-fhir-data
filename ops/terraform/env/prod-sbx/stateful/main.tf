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
    instance_class    = "db.m5.2xlarge"
    iops              = 1000
    allocated_storage = 2000
  }

  db_params = []

  db_import_mode = {
    enabled = false
    maintenance_work_mem = "1048576"
  }

  env_config = {
    env               = "prod-sbx"
    tags              = {application="bfd", business="oeda", stack="prod-sbx", Environment="prod-sbx"}
  }

  victor_ops_url      = var.victor_ops_url

  medicare_opt_out_config = {
    read_roles        = ["arn:aws:iam::755619740999:role/dpc-dev-consent-execution-role"]
    write_roles       = ["arn:aws:iam::755619740999:role/bcda-dev-nfs-instance", "arn:aws:iam::755619740999:role/bcda-test-nfs-instance"]
    admin_users       = ["arn:aws:iam::577373831711:user/DS7H", "arn:aws:iam::577373831711:user/VZG9", "arn:aws:iam::577373831711:user/BYSK"]
  }
}
