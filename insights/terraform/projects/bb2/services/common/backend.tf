provider "aws" {
  version = "~> 3.66"
  region  = "us-east-1"
  default_tags {
    tags = local.tags
  }
}

terraform {
  required_version = "0.13.7"

  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "bfd-insights/bb2/services/common/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
}
