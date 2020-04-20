terraform {
  required_version = "~> 0.12"
  # Use the common terraform bucket for all of BFD's state
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "bfd-insights/foodtruck/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
}

provider "aws" {
  version = "~> 2.57"
  region = "us-east-1"
}