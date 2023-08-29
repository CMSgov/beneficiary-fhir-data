provider "aws" {
  region = "us-east-1"

}

terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "services/migrator/terraform.tfstate"
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
    external = {
      source  = "hashicorp/external"
      version = "~> 2.2"
    }
  }
}
