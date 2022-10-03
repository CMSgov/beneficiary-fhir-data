terraform {
  required_version = "> 0.12.30, < 0.13"
}

provider "aws" {
  version = "~> 4"
  region  = "us-east-1"
}

module "stateful" {
  source = "../../../modules/stateful"

  # feature toggles
  module_features = {
    beta_reader = false
  }

  env_config = {
    env  = "test"
    tags = { application = "bfd", business = "oeda", stack = "test", Environment = "test" }
  }

  victor_ops_url          = var.victor_ops_url
  medicare_opt_out_config = var.medicare_opt_out_config
}
