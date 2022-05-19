terraform {
  required_version = "~> 1.1.9"
  # Use the common terraform bucket for all of BFD's state
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "bfd-insights/bfd/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.12"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}
