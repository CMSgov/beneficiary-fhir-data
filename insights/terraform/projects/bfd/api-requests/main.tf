# Terraform for the API-Requests portion of BFD Insights
#
# NOTE: This module depends on the resources in common.

locals {
  environment  = terraform.workspace
  full_name    = "bfd-insights-${local.project}-${local.environment}"
  database     = local.full_name
  project      = "bfd"
  region       = "us-east-1"
  api_requests_table_name = "${replace(local.full_name, "-", "_")}_api_requests"
  tags         = {
    business    = "OEDA",
    application = "bfd-insights",
    project     = "bfd"
  }
}

# Creates AWS Glue Database named "bfd-insights-bfd-<environment>"
module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = data.aws_s3_bucket.bfd-insights-bucket.bucket
  bucket_cmk = data.aws_kms_key.kms_key.arn
  tags       = local.tags
}
