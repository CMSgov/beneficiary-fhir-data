locals {
  locust_stats_table        = "data"
  locust_stats_db_name      = replace(module.bucket_athena.bucket.bucket, "-", "_")
  locust_stats_crawler_name = "${replace(local.locust_stats_db_name, "_", "-")}-crawler"

  glue_trigger_lambda_name      = "locust-stats-glue-trigger"
  glue_trigger_lambda_full_name = "${local.name_prefix}-${local.glue_trigger_lambda_name}"
  glue_trigger_lambda_src       = replace(local.glue_trigger_lambda_name, "-", "_")
}

module "bucket_athena" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_name        = !var.greenfield ? "${local.name_prefix}-stats" : null
  bucket_prefix      = var.greenfield ? "${local.name_prefix}-stats" : null
  force_destroy      = local.is_ephemeral_env
}

resource "aws_athena_workgroup" "locust_stats" {
  name = module.bucket_athena.bucket.bucket

  configuration {
    enforce_workgroup_configuration = true

    result_configuration {
      output_location = "s3://${module.bucket_athena.bucket.id}/query_results/"

      encryption_configuration {
        encryption_option = "SSE_KMS"
        kms_key_arn       = local.env_key_arn
      }
    }
  }
}

resource "aws_glue_crawler" "locust_stats" {
  name = local.locust_stats_crawler_name
  tags = { Name = local.locust_stats_crawler_name }

  database_name = local.locust_stats_db_name

  role = aws_iam_role.crawler.arn

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_NEW_FOLDERS_ONLY"
  }

  s3_target {
    path = "s3://${module.bucket_athena.bucket.id}/databases/${local.locust_stats_db_name}/${local.locust_stats_table}"
  }

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "LOG"
  }
}

data "archive_file" "locust_stats_glue_trigger_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.glue_trigger_lambda_name}/${local.glue_trigger_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.glue_trigger_lambda_name}/${local.glue_trigger_lambda_src}.py")
    filename = "${local.glue_trigger_lambda_src}.py"
  }
}

resource "aws_cloudwatch_log_group" "locust_stats_glue_trigger" {
  name         = "/aws/lambda/${local.glue_trigger_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "locust_stats_glue_trigger" {
  function_name = local.glue_trigger_lambda_full_name
  description   = "Triggers the ${aws_glue_crawler.locust_stats.name} Glue Crawler to run when new statistics are uploaded to S3"
  tags          = { Name = local.glue_trigger_lambda_full_name }
  kms_key_arn   = local.env_key_arn

  filename         = data.archive_file.locust_stats_glue_trigger_src.output_path
  source_code_hash = data.archive_file.locust_stats_glue_trigger_src.output_base64sha256
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python313-arm64:7"]
  architectures    = ["arm64"]
  handler          = "${local.glue_trigger_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.13"
  timeout          = 300

  environment {
    variables = {
      CRAWLER_NAME  = aws_glue_crawler.locust_stats.name
      DATABASE_NAME = local.locust_stats_db_name
      TABLE_NAME    = local.locust_stats_table
      PARTITIONS    = "hash"
    }
  }

  role = aws_iam_role.locust_stats_glue_trigger.arn
}

resource "aws_lambda_permission" "allow_s3_locust_stats_glue_trigger" {
  statement_id   = "${aws_lambda_function.locust_stats_glue_trigger.function_name}-allow-s3"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.locust_stats_glue_trigger.arn
  principal      = "s3.amazonaws.com"
  source_arn     = module.bucket_athena.bucket.arn
  source_account = local.account_id
}

resource "aws_s3_bucket_notification" "locust_stats_glue_trigger" {
  depends_on = [aws_lambda_permission.allow_s3_locust_stats_glue_trigger]

  bucket = module.bucket_athena.bucket.id

  lambda_function {
    events = [
      "s3:ObjectCreated:*",
    ]
    filter_prefix       = "databases/${local.locust_stats_db_name}/${local.locust_stats_table}/"
    filter_suffix       = ".stats.json"
    id                  = "${aws_lambda_function.locust_stats_glue_trigger.function_name}-s3-notifs"
    lambda_function_arn = aws_lambda_function.locust_stats_glue_trigger.arn
  }
}
