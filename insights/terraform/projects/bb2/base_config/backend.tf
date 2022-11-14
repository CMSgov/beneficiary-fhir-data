terraform {
  # Use the common terraform bucket for all of BFD's state
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "bfd-insights/bb2/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.38"
    }
  }
  required_version = ">= 0.13"
}

provider "aws" {
  region = "us-east-1"
}
