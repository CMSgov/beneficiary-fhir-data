provider "aws" {
  region = "us-east-1"
  default_tags {
    tags = {
      Environment    = "mgmt"
      application    = "bfd"
      business       = "oeda"
      stack          = "bfd-mgmt-backups"
      Terraform      = true
      tf_module_root = "ops/terraform/env/mgmt/backups"
    }
  }
}

terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "environments/mgmt/backups/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.22"
    }
  }
}

