terraform {
  required_version = "> 0.12.30, < 0.13"
}

provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

module "stateless" {
  source = "../../../modules/mgmt_stateless"

  env_config = {
    env  = "mgmt"
    tags = { application = "bfd", business = "oeda", stack = "mgmt", Environment = "mgmt" }
  }

  jenkins_ami           = var.jenkins_ami
  jenkins_key_name      = var.jenkins_key_name
  jenkins_instance_size = var.jenkins_instance_size
  azs                   = var.azs
}
