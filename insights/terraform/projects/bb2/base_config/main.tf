locals {
  tags       = { business = "OEDA", application = "bfd-insights", project = "bb2" }
  project    = "bb2"
  database   = "bb2"
  table      = "events"
  partitions = [{ name = "dt", type = "string", comment = "Approximate delivery time" }]
}

data "aws_caller_identity" "current" {}


## Bucket for the project's data

module "bucket" {
  source         = "../../../modules/bucket"
  name           = local.project
  sensitivity    = "moderate"
  tags           = local.tags
  cross_accounts = var.bb2_cross_accounts
}

data "aws_s3_bucket" "moderate_bucket" {
  bucket = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

data "aws_kms_alias" "moderate_cmk" {
  name = "alias/bfd-insights-moderate-cmk"
}


## Athena workgroup

module "workgroup" {
  source     = "../../../modules/workgroup"
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  name       = local.database
  tags       = local.tags
}


## Database for the project

module "database" {
  source     = "../../../modules/database"
  database   = local.database
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  tags       = local.tags
}


## Tables for the project

locals {
  common_columns = [
    { name = "instance_id", type = "string", comment = "AWS instance id recording the event" },
    { name = "component", type = "string", comment = "Always bb2.web" },
    { name = "vpc", type = "string", comment = "dev, prod, impl, etc." },
    { name = "log_name", type = "string", comment = "BB2 log name" },
    { name = "message", type = "string", comment = "JSON object" },
  ]
}

module "bb2_events" {
  source      = "../../../modules/table"
  database    = module.database.name # adds a dependency
  table       = local.table
  description = "Raw BB2 events"
  bucket      = module.bucket.id
  bucket_cmk  = module.bucket.bucket_cmk
  tags        = local.tags
  partitions  = local.partitions
  columns     = local.common_columns
}


module "firehose" {
  source          = "../../../modules/firehose"
  stream          = local.table
  database        = local.database
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  buffer_interval = 60
  tags            = local.tags
}

## Glue setup

module "glue_jobs" {
  source  = "../../../modules/jobs"
  project = local.project
  tags    = local.tags

  # Setup access to both the bb2 and common moderate bucket
  buckets = [
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn },
    { bucket = data.aws_s3_bucket.moderate_bucket.arn, cmk = data.aws_kms_alias.moderate_cmk.arn }
  ]
}
