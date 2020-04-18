terraform {
  required_version = "~> 0.12"
  # Use the common S3 bucket for all of BFD
  backend "s3" {
    bucket         = "bfd-tf-state"
    key            = "bfd-insights/prod-lake/terraform.tfstate"
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
  tags = {business = "OEDA", product = "bfd-insights"}
}

## S3

module "moderate_bucket" {
  source      = "../modules/bucket"
  sensitivity = "moderate"
  tags        = local.tags
}

## Glue



## Athena


## Firehoses



# Provision Groups with Policies

module "group_analysts" {
  source      = "../modules/iam_group"
  name        = "analysts"
  policy_arns = [
    data.aws_iam_policy.athena_full_access.arn,
    module.kms_policies.use_moderate_cmk_arn,
    module.moderate_bucket.full_arn
  ]
}

module "group_readers" {
  source      = "../modules/iam_group"
  name        = "readers"
  policy_arns = [
  ]
}

# AWS Managed Policies 

data "aws_iam_policy" "athena_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonAthenaFullAccess"
}

data "aws_iam_policy" "quicksight_athena_access" {
  arn = "arn:aws:iam::aws:policy/AWSQuicksightAthenaAccess"
}


## Policies 

module "kms_policies" {
  source      = "../modules/kms_policies"
}



