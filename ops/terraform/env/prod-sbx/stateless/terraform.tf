provider "aws" {
  version = "~> 3.44" # TODO: remove on upgrades beyond v0.12 tf
  region  = "us-east-1"
  default_tags {
    tags = local.default_tags
  }
}

terraform {
  required_version = "> 0.12.30, < 0.13"
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "prod-sbx/stateless/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
}
