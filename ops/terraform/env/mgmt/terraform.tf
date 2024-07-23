provider "aws" {
  region = "us-east-1"
  default_tags {
    tags = {
      Environment    = local.env
      application    = "bfd"
      business       = "oeda"
      stack          = local.env
      Terraform      = true
      tf_module_root = "ops/terraform/env/mgmt"
    }
  }
}

provider "aws" {
  region = "us-west-2"
  alias  = "alt"
  default_tags {
    tags = {
      Environment    = local.env
      application    = "bfd"
      business       = "oeda"
      stack          = local.env
      Terraform      = true
      tf_module_root = "ops/terraform/env/mgmt"
    }
  }
}

terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "environments/mgmt/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.53.0" #"~> 4.67"
    }
  }
}
