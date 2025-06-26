module "terraservice" {
  source               = "../../../terraform-modules/bfd/bfd-terraservice"
  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/02-insights-bb2"
}

locals {
  service               = "insights-bb2"
  env                   = module.terraservice.env
  insights_bucket_env   = var.insights_env != "" ? var.insights_env : local.env
  default_tags          = module.terraservice.default_tags
  tags                  = { business = "OEDA", application = "bfd-insights", project = "bb2" }
  project               = "bb2"
  database              = "bb2"
  table                 = "events"
  partitions            = [{ name = "dt", type = "string", comment = "Approximate delivery time" }]
  bucket_param          = "/bfd/${local.insights_bucket_env}/insights/nonsensitive/bfd-insights-${local.project}"
  moderate_bucket_param = "/bfd/${local.insights_bucket_env}/insights/nonsensitive/bfd-insights-moderate"
  env_key_alias         = module.terraservice.env_key_alias
  env_key_arn           = module.terraservice.env_key_arn
}

data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "bucket_name_param" {
  name = local.bucket_param
}

data "aws_ssm_parameter" "moderate_bucket_name_param" {
  name = local.moderate_bucket_param
}

data "aws_s3_bucket" "bucket" {
  bucket = data.aws_ssm_parameter.bucket_name_param.value
}

data "aws_s3_bucket" "moderate_bucket" {
  bucket = data.aws_ssm_parameter.moderate_bucket_name_param.value
}

## Database for the project

module "database" {
  source     = "../../../terraform-modules/bfd/bfd-insights/database"
  database   = local.database
  bucket     = data.aws_s3_bucket.bucket.id
  bucket_cmk = local.env_key_alias
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
  source      = "../../../terraform-modules/bfd/bfd-insights/table"
  database    = module.database.name # adds a dependency
  table       = local.table
  description = "Raw BB2 events"
  bucket      = data.aws_s3_bucket.bucket.id
  bucket_cmk  = local.env_key_alias
  owner       = "bb2"
  tags        = local.tags
  partitions  = local.partitions
  columns     = local.common_columns
}


module "firehose" {
  source          = "../../../terraform-modules/bfd/bfd-insights/firehose"
  stream          = local.table
  database        = local.database
  bucket          = data.aws_s3_bucket.bucket.id
  bucket_cmk      = local.env_key_alias
  buffer_interval = 60
  tags            = local.tags
}

## Glue setup

module "glue_jobs" {
  source  = "../../../terraform-modules/bfd/bfd-insights/jobs"
  project = local.project
  tags    = local.tags

  # Setup access to both the bb2 and common moderate bucket
  buckets = [
    { bucket = data.aws_s3_bucket.bucket.arn, cmk = local.env_key_arn },
    { bucket = data.aws_s3_bucket.moderate_bucket.arn, cmk = local.env_key_arn }
  ]
}
