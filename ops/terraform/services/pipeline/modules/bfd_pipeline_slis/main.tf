locals {
  env             = terraform.workspace
  region          = data.aws_region.current.name
  resource_prefix = "bfd-${local.env}"

  kms_key_arn = var.aws_kms_key_arn
  kms_key_id  = var.aws_kms_key_id

  lambda_update_slis = "update_slis"
  lambda_repeater    = "repeater"
  lambdas = {
    for label, name in {
      "${local.lambda_update_slis}" = "update-pipeline-slis",
      "${local.lambda_repeater}"    = "pipeline-metrics-repeater"
    } :
    label => {
      name      = name
      full_name = "${local.resource_prefix}-${name}"
      src       = replace(name, "-", "_")
    }
  }

  repeater_invoke_rate = "15 minutes"

  metrics_namespace = "${local.resource_prefix}/bfd-pipeline"
}

resource "aws_lambda_permission" "this" {
  statement_id   = "${local.lambdas[local.lambda_update_slis].full_name}-allow-sns"
  action         = "lambda:InvokeFunction"
  function_name  = aws_lambda_function.this.function_name
  principal      = "sns.amazonaws.com"
  source_arn     = data.aws_sns_topic.this.arn
  source_account = var.account_id
}

resource "aws_sns_topic_subscription" "this" {
  topic_arn = data.aws_sns_topic.this.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.this.arn
}

resource "aws_lambda_function" "this" {
  function_name = local.lambdas[local.lambda_update_slis].full_name

  description = join("", [
    "Puts new CloudWatch Metric Data related to BFD Pipline SLIs whenever a new file is uploaded ",
    "to corresponding Done/Incoming paths in the ${local.env} BFD ETL S3 Bucket ",
    "(${data.aws_s3_bucket.etl.id})"
  ])

  tags = {
    Name = local.lambdas[local.lambda_update_slis].full_name
  }

  kms_key_arn = local.kms_key_arn

  # Ensures that only _one_ instance of this Lambda can run at any given time. This stops the
  # possible duplicate submissions to the first available time metric that could otherwise occur
  # if two instances of this Lambda are invoked close together. This _does_ take from our total
  # reserved concurrent executions, so we should investigate an even more robust method of stopping
  # duplicate submissions
  reserved_concurrent_executions = 1

  filename         = data.archive_file.lambda_src[local.lambda_update_slis].output_path
  source_code_hash = data.archive_file.lambda_src[local.lambda_update_slis].output_base64sha256
  architectures    = ["x86_64"]
  handler          = "${local.lambdas[local.lambda_update_slis].src}.handler"
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

  role = aws_iam_role.lambda[local.lambda_update_slis].arn
}

resource "aws_sqs_queue" "this" {
  name                       = local.lambdas[local.lambda_update_slis].full_name
  visibility_timeout_seconds = 0
  kms_master_key_id          = local.kms_key_id
}

resource "aws_scheduler_schedule_group" "repeater" {
  name = "${local.lambdas[local.lambda_repeater].full_name}-lambda-schedules"
}

resource "aws_scheduler_schedule" "repeater" {
  name       = "${local.lambdas[local.lambda_repeater].full_name}-every-${replace(local.repeater_invoke_rate, " ", "-")}"
  group_name = aws_scheduler_schedule_group.repeater.name

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression = "rate(${local.repeater_invoke_rate})"

  target {
    arn      = aws_lambda_function.repeater.arn
    role_arn = aws_iam_policy.invoke_repeater.arn
  }
}

resource "aws_lambda_function" "repeater" {
  function_name = local.lambdas[local.lambda_repeater].full_name

  description = join("", [
    "Invoked by rate schedules in the ${aws_scheduler_schedule_group.repeater.name} schedule ",
    "group. When invoked, this Lambda re-submits the latest values of several CCW Pipeline ",
    "metrics to corresponding \"-repeating\" CloudWatch Metrics used by the CCW Pipeline SLO Alarms"
  ])

  tags = {
    Name = local.lambdas[local.lambda_repeater].full_name
  }

  kms_key_arn = local.kms_key_arn

  filename         = data.archive_file.lambda_src[local.lambda_repeater].output_path
  source_code_hash = data.archive_file.lambda_src[local.lambda_repeater].output_base64sha256
  architectures    = ["x86_64"]
  handler          = "${local.lambdas[local.lambda_repeater].src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.9"
  timeout          = 300
  environment {
    variables = {
      METRICS_NAMESPACE = local.metrics_namespace
    }
  }

  role = aws_iam_role.lambda[local.lambda_repeater].arn
}
