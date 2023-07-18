data "aws_caller_identity" "current" {}

data "aws_kms_key" "bucket_cmk" {
  key_id = "alias/bfd-insights-bb2-cmk"
}

locals {
  account_id               = data.aws_caller_identity.current.account_id
  lambda_role_arn          = "arn:aws:iam::${local.account_id}:role/service-role/${var.role}"
  lambda_function_name_arn = "arn:aws:lambda:${var.region}:${local.account_id}:function:${var.name}"
  source_dir               = "${path.module}/update_athena_metric_tables/"
}


# Zip File containing Lambda script
data "archive_file" "zip_archive_update_athena_metric_tables" {
  type        = "zip"
  output_path = "${path.module}/lambda_function.zip"
  source_dir  = local.source_dir
  excludes = ["test_lambda_function_local.py",
    "README.md",
    "alter_table_schema_for_new_metrics_added.py",
    "test_run_sql_template_on_athena.py",
    "__pycache__",
  "utils/__pycache__"]
}

resource "aws_lambda_function" "update_athena_metric_tables_lambda" {
  function_name = local.lambda_function_name_arn


  architectures = [
    "x86_64",
  ]
  description                    = var.description
  filename                       = data.archive_file.zip_archive_update_athena_metric_tables.output_path
  handler                        = "lambda_function.lambda_handler"
  layers                         = []
  memory_size                    = 128
  package_type                   = "Zip"
  reserved_concurrent_executions = -1
  role                           = local.lambda_role_arn
  publish                        = false
  runtime                        = "python3.9"
  source_code_hash               = data.archive_file.zip_archive_update_athena_metric_tables.output_base64sha256

  # Set timeout to max.
  timeout = 900

  timeouts {}

  tracing_config {
    mode = "PassThrough"
  }
}
