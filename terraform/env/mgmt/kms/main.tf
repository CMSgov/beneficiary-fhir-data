provider "aws" {
  region = "us-east-1"
}

module "kms" {
  source = "../../modules/resources/kms"
  env = "mgmt"
}
