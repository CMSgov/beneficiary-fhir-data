terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "services/base/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "= 4.17" # Pinning to 4.17; Awaiting resolution on https://github.com/hashicorp/terraform-provider-aws/issues/25335
    }
  }
}
