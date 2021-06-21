terraform {
  required_version = "> 0.12.30, < 0.13"
}

provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

module "stateful" {
  source              = "../../../modules/mgmt_stateful"
  bfd_packages_bucket = var.bfd_packages_bucket
  env_config = {
    env  = "mgmt"
    tags = { application = "bfd", business = "oeda", stack = "mgmt", Environment = "mgmt" }
    azs  = "us-east-1a"
  }
}
