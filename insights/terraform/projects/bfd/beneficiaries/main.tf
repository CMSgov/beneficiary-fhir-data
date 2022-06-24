# Beneficiary and Beneficiary Unique tables and glue jobs to provide BFD Insights with analysis of
# the number of beneficiaries and when they were first seen.
#
# NOTE: This depends on the api-requests section of BFD Insights.

locals {
  environment             = terraform.workspace
  full_name               = "bfd-insights-${local.project}-${local.environment}"
  database                = local.full_name
  project                 = "bfd"
  account_id              = data.aws_caller_identity.current.account_id
  region                  = "us-east-1"
  api_requests_table_name = "${replace(local.full_name, "-", "_")}_api_requests"
  tags         = {
    business    = "OEDA",
    application = "bfd-insights",
    project     = "bfd"
  }
}
