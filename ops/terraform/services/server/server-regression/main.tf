locals {
  default_tags = {
    Environment    = local.seed_env
    Layer          = local.layer
    Name           = "bfd-${local.env}-${local.service}"
    application    = "bfd"
    business       = "oeda"
    role           = local.service
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/server/server-regression"
  }

  env              = terraform.workspace
  established_envs = ["test", "prod-sbx", "prod"]
  seed_env         = one([for x in local.established_envs : x if can(regex("${x}$$", local.env))])
  is_ephemeral_env = local.env != local.seed_env

  account_id = data.aws_caller_identity.current.account_id
  layer      = "app"
  service    = "server-regression"

  insights_db_prefix    = "bfd-insights-bfd"
  insights_table_prefix = "bfd_insights_bfd"
  insights_database     = "${local.insights_db_prefix}-${local.env}"
  insights_table        = "${local.insights_table_prefix}_${replace(local.env, "-", "_")}_${replace(local.service, "-", "_")}"

  vpc_name                   = "bfd-${local.env}-vpc"
  queue_name                 = "bfd-${local.env}-${local.service}"
  pipeline_signal_queue_name = "bfd-${local.env}-${local.service}-signal"

  docker_image_tag = coalesce(var.docker_image_tag_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag.value))

  docker_image_uri = "${data.aws_ecr_repository.ecr.repository_url}:${local.docker_image_tag}"

  lambda_timeout_seconds              = 600
  glue_trigger_lambda_timeout_seconds = 600

  kms_key_arn = data.aws_kms_key.cmk.arn
  kms_key_id  = data.aws_kms_key.cmk.key_id
}

resource "aws_lambda_function" "this" {
  function_name = "bfd-${local.env}-${local.service}"
  description   = "Lambda to run the Locust regression suite against the ${local.env} BFD Server"
  kms_key_arn   = local.kms_key_arn

  image_uri    = local.docker_image_uri
  package_type = "Image"

  memory_size = 2048
  timeout     = local.lambda_timeout_seconds
  environment {
    variables = {
      BFD_ENVIRONMENT          = local.env
      INSIGHTS_BUCKET_NAME     = data.aws_s3_bucket.insights.id,
      SQS_PIPELINE_SIGNAL_NAME = local.pipeline_signal_queue_name
    }
  }

  role = aws_iam_role.this.arn
  vpc_config {
    security_group_ids = [aws_security_group.this.id]
    subnet_ids         = data.aws_subnets.main.ids
  }
}

resource "aws_lambda_event_source_mapping" "this" {
  event_source_arn = aws_sqs_queue.this.arn
  function_name    = aws_lambda_function.this.arn
}

resource "aws_sqs_queue" "this" {
  name                       = local.queue_name
  visibility_timeout_seconds = local.lambda_timeout_seconds
  kms_master_key_id          = local.kms_key_id
}

resource "aws_sqs_queue" "pipeline_signal" {
  name                       = local.pipeline_signal_queue_name
  visibility_timeout_seconds = local.lambda_timeout_seconds
  kms_master_key_id          = local.kms_key_id
}

resource "aws_glue_crawler" "this" {
  name = "${local.insights_database}-${local.service}"
  tags = { Name = "${local.insights_database}-${local.service}", application = "bfd-insights" }

  database_name = local.insights_database

  role = data.aws_iam_role.insights.arn

  lineage_configuration {
    crawler_lineage_settings = "DISABLE"
  }

  recrawl_policy {
    recrawl_behavior = "CRAWL_NEW_FOLDERS_ONLY"
  }

  s3_target {
    path = "s3://${data.aws_s3_bucket.insights.id}/databases/${local.insights_database}/${local.insights_table}"
  }

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "LOG"
  }
}

resource "aws_lambda_permission" "allow_s3_glue_trigger" {
  statement_id   = "bfd-${local.env}-${local.service}-glue-trigger-allow-s3"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.glue_trigger.arn
  principal      = "s3.amazonaws.com"
  source_arn     = data.aws_s3_bucket.insights.arn
  source_account = local.account_id
}

resource "aws_lambda_function" "glue_trigger" {
  description   = "Triggers the bfd-${local.env}-${local.service} Glue Crawler to run when new statistics are uploaded to S3"
  function_name = "bfd-${local.env}-${local.service}-glue-trigger"
  tags          = { Name = "bfd-${local.env}-${local.service}-glue-trigger" }
  kms_key_arn   = local.kms_key_arn

  filename         = data.archive_file.glue_trigger.output_path
  source_code_hash = data.archive_file.glue_trigger.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "glue-trigger.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = local.glue_trigger_lambda_timeout_seconds
  environment {
    variables = {
      CRAWLER_NAME = aws_glue_crawler.this.name
    }
  }

  role = aws_iam_role.glue_trigger.arn
}
