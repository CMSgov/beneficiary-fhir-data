locals {
  tags       = { business = "OEDA", application = "bfd-insights", project = "bcda" }
  project    = "bcda"
  database   = "bcda"
  partitions = [{ name = "dt", type = "string", comment = "Approximate delivery time" }]
  columns = [
    { name = "name", type = "string", comment = "" },
    { name = "timestamp", type = "bigint", comment = "" },
    { name = "json_result", type = "string", comment = "" },
  ]
}

data "aws_caller_identity" "current" {}


## Bucket for the project's data

module "bucket" {
  source         = "../../modules/bucket"
  name           = local.project
  sensitivity    = "moderate"
  tags           = local.tags
  cross_accounts = var.bcda_cross_accounts
}

data "aws_s3_bucket" "moderate_bucket" {
  bucket = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

data "aws_kms_alias" "moderate_cmk" {
  name = "alias/bfd-insights-moderate-cmk"
}

## Athena workgroup

module "workgroup" {
  source     = "../../modules/workgroup"
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  name       = local.database
  tags       = local.tags
}

## Database for the project

module "database" {
  source     = "../../modules/database"
  database   = local.database
  bucket     = module.bucket.id
  bucket_cmk = module.bucket.bucket_cmk
  tags       = local.tags
}

## Glue setup

module "glue_jobs" {
  source  = "../../modules/jobs"
  project = local.project
  tags    = local.tags

  # Setup access to both the BCDA and common moderate bucket
  buckets = [
    { bucket = module.bucket.arn, cmk = module.bucket.bucket_cmk_arn },
    { bucket = data.aws_s3_bucket.moderate_bucket.arn, cmk = data.aws_kms_alias.moderate_cmk.arn }
  ]
}

module "insights_data_table_dev" {
  source      = "../../modules/table"
  database    = module.database.name
  table       = "dev_insights"
  description = "dev insights data"
  bucket      = module.bucket.id
  bucket_cmk  = module.bucket.bucket_cmk
  tags        = local.tags
  partitions  = local.partitions
  columns     = local.columns
}

module "insights_data_table_test" {
  source      = "../../modules/table"
  database    = module.database.name
  table       = "test_insights"
  description = "test insights data"
  bucket      = module.bucket.id
  bucket_cmk  = module.bucket.bucket_cmk
  tags        = local.tags
  partitions  = local.partitions
  columns     = local.columns
}

module "insights_data_table_opensbx" {
  source      = "../../modules/table"
  database    = module.database.name
  table       = "opensbx_insights"
  description = "opensbx insights data"
  bucket      = module.bucket.id
  bucket_cmk  = module.bucket.bucket_cmk
  tags        = local.tags
  partitions  = local.partitions
  columns     = local.columns
}

module "insights_data_table_prod" {
  source      = "../../modules/table"
  database    = module.database.name
  table       = "prod_insights"
  description = "prod insights data"
  bucket      = module.bucket.id
  bucket_cmk  = module.bucket.bucket_cmk
  tags        = local.tags
  partitions  = local.partitions
  columns     = local.columns
}

# lambda to reload partitions
resource "aws_lambda_function" "bcda_load_partitions" {
  filename         = "${path.module}/lambda.zip"
  description      = "Loads partitions for tables"
  function_name    = "bcda_load_partitions"
  role             = aws_iam_role.bcda_load_partitions.arn
  handler          = "index.handler"
  source_code_hash = data.archive_file.bcda_load_partitions.output_base64sha256
  runtime          = "nodejs12.x"
  timeout          = 90
}

resource "aws_cloudwatch_log_group" "bcda_load_partitions" {
  name              = "/aws/lambda/bcda_load_partitions"
  retention_in_days = 14
}
resource "aws_iam_role" "bcda_load_partitions" {
  name_prefix        = "BcdaLoadPartitions-"
  assume_role_policy = data.aws_iam_policy_document.bcda_load_partitions.json
}

data "aws_iam_policy_document" "bcda_load_partitions" {
  statement {
    effect = "Allow"
    actions = [
      "sts:AssumeRole",
    ]

    principals {
      type = "Service"
      identifiers = [
        "lambda.amazonaws.com"
      ]
    }
  }
}

data "aws_iam_policy" "glue_service" {
  arn = "arn:aws:iam::aws:policy/service-role/AWSGlueServiceRole"
}

data "aws_iam_policy" "athena_service" {
  arn = "arn:aws:iam::aws:policy/AmazonAthenaFullAccess"
}

data "aws_iam_policy" "s3_service" {
  arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

data "aws_iam_policy" "glue_console_service" {
  arn = "arn:aws:iam::aws:policy/AWSGlueConsoleFullAccess"
}

locals {
  lambda_role_policies = [
    module.glue_jobs.s3_access_policy_arn,
    data.aws_iam_policy.glue_service.arn,
    data.aws_iam_policy.athena_service.arn,
    data.aws_iam_policy.s3_service.arn,
    data.aws_iam_policy.glue_console_service.arn
  ]
}

resource "aws_iam_role_policy_attachment" "bcda_load_partitions" {
  count      = length(local.lambda_role_policies)
  role       = aws_iam_role.bcda_load_partitions.name
  policy_arn = local.lambda_role_policies[count.index]
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
