provider "aws" {
  region = "us-east-1"
}

module "elb" {
  source = "../../modules/elb"

  name               = "bfd-mgmt-clb-1"
  env                = "mgmt"
  ssl_certificate_id = "arn:aws:acm:us-east-1:577373831711:certificate/2f479dcc-a28b-41ad-9f7b-1abd8a72b905"
}
