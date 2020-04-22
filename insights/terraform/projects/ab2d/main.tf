locals {
  tags      = {business = "OEDA", application = "bfd-insights", project="ab2d"}
  database  = "ab2d"
  table     = "dummy"
  sensitivity = "moderate"
}

## Firehose for gathering purchase data

module "firehose" {
  source          = "../../modules/firehose"
  stream          = local.table
  database        = local.database
  sensitivity     = local.sensitivity
  buffer_interval = 60
  tags            = local.tags
}


## Database for the purchase data

module "database" {
  source          = "../../modules/database"
  database        = local.database
  sensitivity     = local.sensitivity
  tags            = local.tags
}

module "table" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = local.table
  sensitivity     = local.sensitivity
  tags            = local.tags
  partitions      = module.firehose.partitions
  columns = [
    {name="timestamp", type="timestamp", comment="Time of purchase ISO format"},
  ] 
}