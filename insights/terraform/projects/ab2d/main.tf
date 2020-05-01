locals {
  tags      = {business = "OEDA", application = "bfd-insights", project="ab2d"}
  project   = "ab2d"
  database  = "ab2d"
  table     = "dummy"
}


## Bucket for the project's data

module "bucket" {
  source          = "../../modules/bucket"
  name            = local.project
  sensitivity     = "moderate"
  tags            = local.tags  
  cross_accounts  = ["777200079629","595094747606", "330810004472"]
}


## Athena workgroup

module "workgroup" {
  source          = "../../modules/workgroup"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  name            = local.database
  tags            = local.tags
}

## Database for the project

module "database" {
  source          = "../../modules/database"
  database        = local.database
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
}

