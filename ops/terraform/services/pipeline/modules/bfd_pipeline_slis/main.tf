locals {
  env    = terraform.workspace
  region = data.aws_region.current.name

  kms_key_arn = var.aws_kms_key_arn
  kms_key_id  = var.aws_kms_key_id

  lambda_full_name = "bfd-${local.env}-update-pipeline-slis"

  metrics_namespace = "bfd-${local.env}/bfd-pipeline"
}

resource "aws_lambda_permission" "this" {
  statement_id   = "${local.lambda_full_name}-allow-s3"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.this.arn
  principal      = "s3.amazonaws.com"
  source_arn     = data.aws_s3_bucket.etl.arn
  source_account = var.account_id
}

resource "aws_lambda_function" "this" {
  function_name = local.lambda_full_name

  description = join("", [
    "Puts new CloudWatch Metric Data related to BFD Pipline SLIs whenever a new file is uploaded ",
    "to corresponding Done/Incoming paths in the ${local.env} BFD ETL S3 Bucket ",
    "(${data.aws_s3_bucket.etl.id})"
  ])

  tags = {
    Name = local.lambda_full_name
  }

  kms_key_arn = local.kms_key_arn

  # Ensures that only _one_ instance of this Lambda can run at any given time. This stops the
  # possible duplicate submissions to the first available time metric that could otherwise occur
  # if two instances of this Lambda are invoked close together. This _does_ take from our total
  # reserved concurrent executions, so we should investigate an even more robust method of stopping
  # duplicate submissions
  # TODO: Investigate other methods of stopping duplicate sample submissions to first available metric
  reserved_concurrent_executions = 1

  filename         = data.archive_file.lambda_src.output_path
  source_code_hash = data.archive_file.lambda_src.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "update_pipeline_slis.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = 300
  environment {
    variables = {
      METRICS_NAMESPACE   = local.metrics_namespace
      ETL_BUCKET_ID       = data.aws_s3_bucket.etl.id
      SENTINEL_QUEUE_NAME = aws_sqs_queue.this.name
    }
  }

  role = aws_iam_role.this.arn
}

resource "aws_sqs_queue" "this" {
  name                       = local.lambda_full_name
  visibility_timeout_seconds = 0
  kms_master_key_id          = local.kms_key_id
}
