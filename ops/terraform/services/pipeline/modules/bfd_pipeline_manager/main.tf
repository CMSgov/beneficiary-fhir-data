locals {
  env    = terraform.workspace
  region = data.aws_region.current.name

  kms_key_arn      = data.aws_kms_key.env_cmk.arn
  mgmt_kms_key_arn = data.aws_kms_key.mgmt_cmk.arn

  lambda_full_name = "bfd-${local.env}-pipeline-manager"
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
    "paths of the ${local.env} S3 Bucket, this Lambda applies ASG Scheduled Actions to the ",
    "${var.ccw_pipeline_asg_details.name} ASG for auto-scaling"
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
      BFD_ENVIRONMENT = local.env
      ETL_BUCKET_ID   = data.aws_s3_bucket.etl.id
    }
  }

  role = aws_iam_role.this.arn
}
