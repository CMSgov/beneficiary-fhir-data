provider "aws" {
  region = "us-east-1"
}

module "elb" {
  source = "../../modules/elb"

  name               = "bfd-mgmt-clb-1"
  env                = "mgmt"
  ssl_certificate_id = ""
}
