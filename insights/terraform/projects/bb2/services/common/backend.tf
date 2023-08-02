provider "aws" {
  region = "us-east-1"
  default_tags {
    tags = local.tags
  }
}

terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "bfd-insights/bb2/services/common/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.7.0"
    }

    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4.0"
    }
  }
}
