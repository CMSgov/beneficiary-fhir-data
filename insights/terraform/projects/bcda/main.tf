locals {
  project  = "bcda"
  database = "bcda"
  owner    = "bcda"
  partitions = [
    { name = "dt", type = "string", comment = "Approximate delivery time" }
  ]
  columns = [
    { name = "name", type = "string", comment = "" },
    { name = "timestamp", type = "bigint", comment = "" },
    { name = "json_result", type = "string", comment = "" },
  ]

  # List of arns for cross account access to the bucket. This value is populated from the SSM parameter store and is
  # encrypted with this project's CMK. It is currently managed manually, but will be managed by Terraform in the future.
  cross_account_arns = tolist(split(",", data.aws_ssm_parameter.cross_account_arns.value))

  # The following tags get applied to all resources in this project. They are used to enforce ABAC policies, so modify
  # with caution. To append custom tags to specific resource, simply add them to the resource's tags map, not here.
  default_tags = {
    Terraform   = true
    business    = "oeda"
    application = "bfd"
    project     = "insights"
    subproject  = "bcda"
    owner       = "bcda"
  }
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# As of 08/2023 this parameter is currently managed manually. It was populated from a now deprecated tfvars file.
data "aws_ssm_parameter" "cross_account_arns" {
  name = "/bcda/global/sensitive/insights/bucket/cross_account_arns"
  with_decryption = true
}

data "aws_s3_bucket" "moderate_bucket" {
  bucket = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}
data "aws_kms_alias" "moderate_cmk" {
  name = "alias/bfd-insights-moderate-cmk"
}

## Bucket for the project's data
module "bucket" {
  source         = "../../modules/bucket"
  name           = local.project
  sensitivity    = "moderate"
  cross_accounts = local.cross_account_arns
}

## Athena workgroup
module "workgroup" {
  source     = "../../modules/workgroup"
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  name       = local.database
}

## Glue Catalog Database for the project
module "database" {
  source     = "../../modules/database"
  database   = local.database
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
}

## Glue setup
module "glue_jobs" {
  source  = "../../modules/jobs"
  project = local.project

  # Setup access to both the BCDA and common moderate bucket
  buckets = [
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn },
    { bucket = data.aws_s3_bucket.moderate_bucket.arn, cmk = data.aws_kms_alias.moderate_cmk.arn }
  ]
}

# Tables
module "insights_data_table_dev" {
  source             = "../../modules/table"
  owner              = local.owner
  database           = module.database.name
  table              = "dev_insights"
  description        = "dev insights data"
  bucket             = module.bucket.id
  bucket_cmk         = module.bucket.bucket_cmk
  partitions         = local.partitions
  columns            = local.columns
  storage_compressed = false
}

module "insights_data_table_test" {
  source             = "../../modules/table"
  owner              = local.owner
  database           = module.database.name
  table              = "test_insights"
  description        = "test insights data"
  bucket             = module.bucket.id
  bucket_cmk         = module.bucket.bucket_cmk
  partitions         = local.partitions
  columns            = local.columns
  storage_compressed = false
}

module "insights_data_table_opensbx" {
  source             = "../../modules/table"
  owner              = local.owner
  database           = module.database.name
  table              = "opensbx_insights"
  description        = "opensbx insights data"
  bucket             = module.bucket.id
  bucket_cmk         = module.bucket.bucket_cmk
  partitions         = local.partitions
  columns            = local.columns
  storage_compressed = false
}

module "insights_data_table_prod" {
  source             = "../../modules/table"
  owner              = local.owner
  database           = module.database.name
  table              = "prod_insights"
  description        = "prod insights data"
  bucket             = module.bucket.id
  bucket_cmk         = module.bucket.bucket_cmk
  partitions         = local.partitions
  columns            = local.columns
  storage_compressed = false
}

# lambda to reload partitions
resource "aws_lambda_function" "bcda_load_partitions" {
  filename         = "${path.module}/lambda.zip"
  description      = "Loads partitions for tables"
  function_name    = "bcda_load_partitions"
  role             = aws_iam_role.bcda_load_partitions.arn
  handler          = "index.handler"
  source_code_hash = data.archive_file.bcda_load_partitions.output_base64sha256
  runtime          = "nodejs16.x"
  timeout          = 90
}

resource "aws_cloudwatch_log_group" "bcda_load_partitions" {
  name              = "/aws/lambda/bcda_load_partitions"
  retention_in_days = 14
}

resource "aws_iam_role" "bcda_load_partitions" {
  name_prefix = "BcdaLoadPartitions-"
  path = var.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Action = "sts:AssumeRole"
          Effect = "Allow"
          Principal = {
            Service = "lambda.amazonaws.com"
          }
        }
      ]
    }
  )

  # we are not using these policies anywhere else, so just inline them here instead of the previous use of data sources
  # and policy attachments (does the same thing, but less verbose)
  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole",
    "arn:aws:iam::aws:policy/AmazonAthenaFullAccess",
    "arn:aws:iam::aws:policy/AmazonS3FullAccess",
    "arn:aws:iam::aws:policy/AWSGlueConsoleFullAccess",
    module.glue_jobs.s3_access_policy_arn,
  ]
}

data "archive_file" "bcda_load_partitions" {
  type        = "zip"
  output_path = "${path.module}/lambda.zip"
  source_dir  = "${path.module}/lambda"
}

resource "aws_cloudwatch_event_rule" "main" {
  name                = "bcda_load_partitions"
  description         = "Runs everyday at 3AM GMT"
  schedule_expression = "cron(0 3 ? * * *)"
  is_enabled          = true
}

resource "aws_cloudwatch_event_target" "main" {
  rule = aws_cloudwatch_event_rule.main.name
  arn  = aws_lambda_function.bcda_load_partitions.arn
}

resource "aws_lambda_permission" "main" {
  statement_id_prefix = "BcdaLoadPartitions-AllowExecutionFromCloudWatch-"
  action              = "lambda:InvokeFunction"
  function_name       = "bcda_load_partitions"
  principal           = "events.amazonaws.com"
  source_arn          = aws_cloudwatch_event_rule.main.arn
}
