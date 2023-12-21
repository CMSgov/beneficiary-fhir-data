provider "aws" {
  region = "us-east-1"
  default_tags {
    tags = local.default_tags
  }
}
provider "aws" {
  region = "us-west-2"
  alias  = "alt"
  default_tags {
    tags = local.default_tags
  }
}

terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "services/pipeline/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.22"
    }
  }
}
