locals {
  tags       = { business = "OEDA", application = "bfd-insights", project = "bcda" }
  project    = "bcda"
  database   = "bcda"
  partitions = [{ name = "dt", type = "string", comment = "Approximate delivery time" }]
}

data "aws_caller_identity" "current" {}


## Bucket for the project's data

module "bucket" {
  source      = "../../modules/bucket"
  name        = local.project
  sensitivity = "moderate"
  tags        = local.tags
  cross_accounts = [
    "arn:aws:iam::755619740999:role/BcdaBfdInsightsRole"
  ]
}

data "aws_s3_bucket" "moderate_bucket" {
  bucket = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

data "aws_kms_alias" "moderate_cmk" {
  name = "alias/bfd-insights-moderate-cmk"
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

## Glue setup

module "glue_jobs" {
  source          = "../../modules/jobs"
  project         = local.project
  tags            = local.tags

  # Setup access to both the AB2D and common moderate bucket
  buckets         = [
    {bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn},
    {bucket = data.aws_s3_bucket.moderate_bucket.arn, cmk = data.aws_kms_alias.moderate_cmk.arn }
  ]
}

module "get_num_jobs_opensbx" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "opensbx_get_num_jobs" # "${environment}_${name of job}"
  description     = "gets number of data requests (jobs)"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = [
    {name="name", type="string", comment=""},
    {name="timestamp", type="bigint", comment=""},
    {name="data", type="array<struct<count:string>>", comment=""},
  ]
}

module "get_num_queued_tasks_opensbx" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "opensbx_get_num_queued_tasks" # "${environment}_${name of job}"
  description     = "gets number of queued job tasks"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = [
    {name="name", type="string", comment=""},
    {name="timestamp", type="bigint", comment=""},
    {name="data", type="array<struct<count:string>>", comment=""},
  ]
}