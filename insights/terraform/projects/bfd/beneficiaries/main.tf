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
  account_id   = data.aws_caller_identity.current.account_id
  region       = "us-east-1"
}
