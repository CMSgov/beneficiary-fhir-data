locals {
  tags      = {business = "OEDA", application = "bfd-insights", project="bfd"}
  database  = "bfd"
  table     = "beneficiaries"
}

## Bucket for the project
module "bucket" {
  source          = "../../modules/bucket"
  name            = local.database
  sensitivity     = "high"
  tags            = local.tags
}
