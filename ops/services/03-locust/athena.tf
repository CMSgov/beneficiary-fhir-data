locals {
  locust_stats_bucket       = "${local.name_prefix}-stats"
  locust_stats_table        = "data"
  locust_stats_db_name      = replace(local.locust_stats_bucket, "-", "_")
  locust_stats_crawler_name = "${replace(local.locust_stats_db_name, "_", "-")}-crawler"

  glue_trigger_lambda_name      = "locust-stats-trigger-crawler"
  glue_trigger_lambda_full_name = "${local.name_prefix}-${local.glue_trigger_lambda_name}"
}

module "bucket_athena" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_prefix      = local.locust_stats_bucket
  force_destroy      = local.is_ephemeral_env

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/bucket"
}

resource "aws_athena_workgroup" "locust_stats" {
  name = local.locust_stats_bucket

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

module "locust_stats_trigger" {
  source = "../../terraform-modules/general/trigger-glue-crawler"

  iam_path                     = local.iam_path
  iam_permissions_boundary_arn = local.permissions_boundary_arn
  kms_key_arn                  = local.env_key_arn

  lambda_name = local.glue_trigger_lambda_full_name

  crawler_name  = aws_glue_crawler.locust_stats.name
  crawler_arn   = aws_glue_crawler.locust_stats.arn
  database_name = local.locust_stats_db_name
  table_name    = local.locust_stats_table
  partitions    = ["hash"]
}

resource "aws_lambda_permission" "allow_s3_locust_stats_glue_trigger" {
  statement_id   = "${module.locust_stats_trigger.lambda.function_name}-allow-s3"
  action         = "lambda:InvokeFunction"
  function_name  = module.locust_stats_trigger.lambda.arn
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
    id                  = "${module.locust_stats_trigger.lambda.function_name}-s3-notifs"
    lambda_function_arn = module.locust_stats_trigger.lambda.arn
  }
}
