locals {
  environment  = terraform.workspace
  tags         = {
    business    = "OEDA",
    application = "bfd-insights",
    project     = "bfd"
  }
  database     = "bfd-${local.environment}"
  project      = "bfd"
  account_id   = data.aws_caller_identity.current.account_id
  region       = "us-east-1"
}
