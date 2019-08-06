provider "aws" {
  region = "us-east-1"
}

module "dns" {
  source = "../../modules/dns"

  vpc_id = "vpc-08141e13c2750df9f"
  env    = "mgmt"
}
