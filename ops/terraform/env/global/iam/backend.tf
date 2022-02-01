terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "global/iam/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = true
    kms_key_id     = "alias/bfd-tf-state"
  }
}
