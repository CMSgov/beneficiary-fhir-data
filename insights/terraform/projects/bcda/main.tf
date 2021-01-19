locals {
  tags       = { business = "OEDA", application = "bfd-insights", project = "bcda" }
  project    = "bcda"
  database   = "bcda"
  partitions = [{ name = "dt", type = "string", comment = "Approximate delivery time" }]
}

data "aws_caller_identity" "current" {}


## Bucket for the project's data

module "bucket" {
  source      = "../../modules/bucket"
  name        = local.project
  sensitivity = "moderate"
  tags        = local.tags
  cross_accounts = [
    "arn:aws:iam::755619740999:role/BcdaBfdInsightsRole"
  ]
}

data "aws_s3_bucket" "moderate_bucket" {
  bucket = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

data "aws_kms_alias" "moderate_cmk" {
  name = "alias/bfd-insights-moderate-cmk"
}

module "workgroup" {
  source          = "../../modules/workgroup"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  name            = local.database
  tags            = local.tags
}