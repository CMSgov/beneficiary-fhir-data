locals {
  env    = terraform.workspace
  region = data.aws_region.current.name

  kms_key_arn      = data.aws_kms_key.env_cmk.arn
  mgmt_kms_key_arn = data.aws_kms_key.mgmt_cmk.arn

  lambda_full_name = "bfd-${local.env}-pipeline-manager"

  jenkins_job_queue_arn   = data.aws_sqs_queue.jenkins_job_queue.arn
  jenkins_job_queue_name  = data.aws_sqs_queue.jenkins_job_queue.name
  jenkins_target_job_name = "bfd-deploy-pipeline-terraservice"
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
    "Invoked whenever a new file appears in either the root or Synthetic Done/ and Incoming/ ",
    "paths of the ${local.env} S3 Bucket, this Lambda runs the ${local.jenkins_target_job_name} ",
    "Jenkins job through the ${local.jenkins_job_queue_name} SQS queue"
  ])

  tags = {
    Name = local.lambda_full_name
  }

  kms_key_arn      = local.kms_key_arn
  filename         = data.archive_file.lambda_src.output_path
  source_code_hash = data.archive_file.lambda_src.output_base64sha256
  architectures    = ["x86_64"]
  handler          = "pipeline_manager.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = 300

  environment {
    variables = {
      BFD_ENVIRONMENT          = local.env
      JENKINS_TARGET_JOB_NAME  = local.jenkins_target_job_name
      JENKINS_JOB_RUNNER_QUEUE = local.jenkins_job_queue_name
      ONGOING_LOAD_QUEUE       = aws_sqs_queue.this.name
      ETL_BUCKET_ID            = data.aws_s3_bucket.etl.id
    }
  }

  role = aws_iam_role.this.arn
}

resource "aws_sqs_queue" "this" {
  name                       = local.lambda_full_name
  visibility_timeout_seconds = 0
  kms_master_key_id          = local.kms_key_arn
}
