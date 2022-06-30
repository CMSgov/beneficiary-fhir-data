# Beneficiary and Beneficiary Unique tables and glue jobs to provide BFD Insights with analysis of
# the number of beneficiaries and when they were first seen.
#
# NOTE: This depends on the api-requests section of BFD Insights.

locals {
  environment          = terraform.workspace
  full_name            = "bfd-insights-${local.project}-${local.environment}"
  full_name_underscore = replace(local.full_name, "-", "_")
  database             = local.full_name
  project              = "bfd"
  region               = "us-east-1"
  account_id           = data.aws_caller_identity.current.account_id
  glue_workflow_name   = "${local.full_name}-api-requests-workflow"

  tags = {
    business    = "OEDA",
    application = "bfd-insights",
    project     = "bfd"
  }
}
