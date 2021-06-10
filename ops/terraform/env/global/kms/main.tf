provider "aws" {
  version = "~> 3.44.0"
  region  = "us-east-1"
}

resource "aws_kms_key" "state_kms_key" {
  description             = "bfd-tf-state"
  deletion_window_in_days = 10
  enable_key_rotation     = true
}

resource "aws_kms_alias" "state_kms_alias" {
  name          = "alias/bfd-tf-state"
  target_key_id = aws_kms_key.state_kms_key.key_id
}
