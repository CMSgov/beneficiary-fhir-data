# Terraform for the API-Requests portion of BFD Insights
#
# NOTE: This module depends on the resources in common.

locals {
  environment          = terraform.workspace
  full_name            = "bfd-insights-${local.project}-${local.environment}"
  full_name_underscore = replace(local.full_name, "-", "_")
  database             = local.full_name
  project              = "bfd"
  region               = "us-east-1"
  account_id           = data.aws_caller_identity.current.account_id

  tags = {
    business    = "OEDA",
    application = "bfd-insights",
    project     = "bfd"
    environment = local.environment
  }
}
