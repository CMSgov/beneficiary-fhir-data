terraform {
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "security/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
}
