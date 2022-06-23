# Terraform for the API-Requests portion of BFD Insights
#
# NOTE: This module depends on the resources in common.

locals {
  environment  = terraform.workspace
  tags         = {
    business    = "OEDA",
    application = "bfd-insights",
    project     = "bfd"
  }
  full_name    = "bfd-insights-${local.project}-${local.environment}"
  database     = local.full_name
  project      = "bfd"
  region       = "us-east-1"
}

module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk = data.aws_kms_key.kms_key.arn
  tags       = local.tags
}
