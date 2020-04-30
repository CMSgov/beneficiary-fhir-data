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
}


## Firehose for gathering project data

module "firehose" {
  source          = "../../modules/firehose"
  stream          = local.table
  database        = local.database
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  buffer_interval = 60
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


## Dummy table for the project

module "table" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = local.table
  description     = "test table for the AB2D projects"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = module.firehose.partitions
  columns = [
    {name="timestamp", type="timestamp", comment="Time of purchase ISO format"},
  ] 
}