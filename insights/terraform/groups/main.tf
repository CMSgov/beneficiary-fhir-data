terraform {
  required_version = "~> 0.12"
  # Use the common terraform bucket for all of BFD's state
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "bfd-insights/groups/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "bfd-tf-table"
    encrypt        = "1"
    kms_key_id     = "alias/bfd-tf-state"
  }
}

provider "aws" {
  version = "~> 2.57"
  region = "us-east-1"
}

locals {
  # Add new group members here
  member_map  = {
    analysts  = ["HWRI", "SKKH"]
    readers   = []
  }
}

# Add to the IAM groups

data "aws_iam_group" "groups" {
  for_each    = local.member_map
  group_name  = "bfd-insights-${each.key}"
}

resource "aws_iam_group_membership" "team" {
  for_each    = local.member_map
  name        = "bfd-insights-${each.key}-team"
  group       = data.aws_iam_group.groups[each.key].group_name
  users       = each.value
}


# Create ad-hoc folders for analysts

data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "main" {
  bucket        = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_object" "user_folder" {
  for_each      = toset(local.member_map["analysts"])
  bucket        = data.aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "users/${each.value}/"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_object" "user_output_folder" {
  depends_on    = [aws_s3_bucket_object.user_folder]
  for_each      = toset(local.member_map["analysts"])
  bucket        = data.aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "users/${each.value}/query_results"

  lifecycle {
    prevent_destroy = true
  }
}





