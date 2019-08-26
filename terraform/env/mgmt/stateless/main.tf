terraform {
  required_version = "~> 0.12"
}

provider "aws" {
  version = "~> 2.23"
  region = "us-east-1"
}

module "stateless" {
  source = "../../../modules/mgmt_stateless"

  env_config = {
    env               = "mgmt"
    tags              = {application="bfd", business="oeda", stack="mgmt", Environment="mgmt"}
  }  

  jenkins_ami            = var.jenkins_ami
  vpn_security_group_id  = var.vpn_security_group_id
  jenkins_key_name       = var.jenkins_key_name
  
}
