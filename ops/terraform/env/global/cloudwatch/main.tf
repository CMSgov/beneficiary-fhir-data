terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "global/cloudwatch/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
}

module "stateless" {
  source = "../../../modules/resources/bfd_cw_dashboards"
}

