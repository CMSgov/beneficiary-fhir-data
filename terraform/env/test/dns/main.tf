provider "aws" {
  region = "us-east-1"
}

module "dns" {
  source = "../../modules/dns"

  vpc_id = ""
  env    = ""
}
