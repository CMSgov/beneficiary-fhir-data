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
    env               = "test"
    tags              = {application="bfd", business="oeda", stack="test", Environment="test"}
  }  

  jenkins_ami            = var.jenkins_ami
  vpn_security_group_id  = var.vpn_security_group_id
  jenkins_tls_cert_arn   = var.jenkins_tls_cert_arn
  jenkins_key_name       = var.jenkins_key_name
  
}
