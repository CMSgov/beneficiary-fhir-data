locals {
  env              = terraform.workspace
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", local.env))])

  kms_key_arn = data.aws_kms_key.cmk.arn
  kms_key_id  = data.aws_kms_key.cmk.key_id

  lambda_full_name = "${var.name_prefix}-trigger-glue-crawler"
}

resource "aws_lambda_permission" "this" {
  statement_id   = "${local.lambda_full_name}-allow-s3"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.this.arn
  principal      = "s3.amazonaws.com"
  source_arn     = var.insights_bucket_arn
  source_account = var.account_id
}

resource "aws_lambda_function" "this" {
  function_name = local.lambda_full_name

  description = join("", [
    "Triggers the ${var.glue_crawler_name} Glue Crawler to run when new parquet files are uploaded ",
    "to the API requests' Glue Table path in S3 if the file is part of a new partition."
  ])

  tags = {
    Name = local.lambda_full_name
  }

  kms_key_arn = local.kms_key_arn

  filename         = data.archive_file.lambda_src.output_path
  source_code_hash = data.archive_file.lambda_src.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "trigger_glue_crawler.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.11"
  timeout          = 520 # 520 seconds gives enough time for backoff retries to be attempted
  environment {
    variables = {
      CRAWLER_NAME       = var.glue_crawler_name
      GLUE_DATABASE_NAME = var.glue_database
      GLUE_TABLE_NAME    = var.glue_table
    }
  }

  role = aws_iam_role.this.arn
}
