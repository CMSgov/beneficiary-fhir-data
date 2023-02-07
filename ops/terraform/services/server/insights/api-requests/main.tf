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

module "trigger_glue_crawler" {
  source = "./modules/trigger_glue_crawler"

  account_id          = local.account_id
  insights_bucket_arn = data.aws_s3_bucket.bfd-insights-bucket.arn
  name_prefix         = local.full_name
  glue_database       = module.database.name
  glue_table          = module.glue-table-api-requests.name
  glue_crawler_name   = aws_glue_crawler.glue-crawler-api-requests.name
  glue_crawler_arn    = aws_glue_crawler.glue-crawler-api-requests.arn
}
