locals {
  tags      = {business = "OEDA", application = "bfd-insights", project="ab2d"}
  project   = "ab2d"
  database  = "ab2d"
  partitions = [{name="dt", type="string", comment="Approximate delivery time"}]
}


## Bucket for the project's data

module "bucket" {
  source          = "../../modules/bucket"
  name            = local.project
  sensitivity     = "moderate"
  tags            = local.tags  
  cross_accounts  = [
    # Needed for firehoses
    "arn:aws:iam::777200079629:role/Ab2dBfdInsightsRole",  
    "arn:aws:iam::349849222861:role/Ab2dBfdInsightsRole",
    "arn:aws:iam::330810004472:role/Ab2dBfdInsightsRole",  
    # Needed for developers
    "arn:aws:iam::777200079629:role/ab2d-spe-developer",   
    "arn:aws:iam::349849222861:role/ab2d-spe-developer",
    "arn:aws:iam::330810004472:role/ab2d-spe-developer"
  ]
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

## Tables for the project

locals {
  common_columns = [
    {name="environment", type="string", comment="dev, prod, impl, etc."},
    {name="id", type="bigint", comment="a unique db id if it exists"},
    {name="aws_id", type="string", comment="this is the AWS request id that may be saved back to the DB"},
    {name="time_of_event", type="timestamp", comment="ISO format in UTC timezone"},
    {name="user", type="string", comment="a user identifier"},
    {name="job_id", type="string", comment="the batch job id"}  
  ]
}

module "api_request_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "api_request_event"
  description     = "Raw API request events"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="url", type="string", comment=""},
    {name="ip_address", type="string", comment=""},
    {name="token_hash", type="string", comment=""},
    {name="request_id", type="string", comment=""}
  ])
}

module "api_response_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "api_response_event"
  description     = "Raw API response events"
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="response_code", type="int", comment=""},
    {name="response_string", type="string", comment=""},
    {name="description", type="string", comment=""},
    {name="request_id", type="string", comment=""},
  ])
}

module "beneficiary_search_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "beneficiary_search_event"
  description     = ""
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="response", type="string", comment=""},
    {name="response_date", type="string", comment=""},
    {name="bene_id", type="string", comment=""},
    {name="contract_num", type="string", comment=""},
  ])
}

module "contract_bene_search_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "contract_bene_search_event"
  description     = ""
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="contract_number", type="string", comment=""},
    {name="num_in_contract", type="int", comment=""},
    {name="num_searched", type="int", comment=""},
    {name="num_opted_out", type="int", comment=""},
    {name="num_errors", type="int", comment=""},
  ])
}

module "error_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "error_event"
  description     = ""
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="error_type", type="string", comment=""},
    {name="description", type="string", comment=""},
  ])
}

module "file_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "file_event"
  description     = ""
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="file_name", type="string", comment=""},
    {name="status", type="string", comment=""},
    {name="file_size", type="bigint", comment=""},
    {name="file_hash", type="string", comment=""},
  ])
}

module "job_status_change_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "job_status_change_event"
  description     = ""
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="old_status", type="string", comment=""},
    {name="new_status", type="string", comment=""},
    {name="description", type="string", comment=""},
  ])
}

module "reload_event" {
  source          = "../../modules/table"
  database        = module.database.name          # adds a dependency
  table           = "reload_event"
  description     = ""
  bucket          = module.bucket.id
  bucket_cmk      = module.bucket.bucket_cmk
  tags            = local.tags
  partitions      = local.partitions
  columns = concat(local.common_columns, [
    {name="file_type", type="string", comment=""},
    {name="file_name", type="string", comment=""},
    {name="number_loaded", type="int", comment=""},
  ])
}