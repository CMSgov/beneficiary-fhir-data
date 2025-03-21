# TODO: Remove this, and the Lambda source, when the server-regression Terraservice and Lambda are migrated
locals {
  rw_lambda_name      = "regression-wrapper"
  rw_lambda_full_name = "${local.name_prefix}-${local.rw_lambda_name}"
  rw_lambda_src       = replace(local.rw_lambda_name, "-", "_")
}

data "aws_sqs_queue" "regression_invoke" {
  name = "bfd-${local.env}-server-regression"
}

data "aws_sqs_queue" "regression_result" {
  name = "bfd-${local.env}-server-regression-signal"
}

data "archive_file" "regression_wrapper_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.rw_lambda_name}/${local.rw_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.rw_lambda_name}/${local.rw_lambda_src}.py")
    filename = "${local.rw_lambda_src}.py"
  }
}

resource "aws_lambda_function" "regression_wrapper" {
  function_name = local.rw_lambda_full_name

  description = join("", [
    "Invoked on the AfterAllowTestTraffic CodeDeploy Lifecycle Event. This Lambda is a wrapper ",
    "around the existing Regression Suite Lambda to make it work with CodeDeploy"
  ])

  kms_key_arn      = data.aws_kms_alias.env_cmk.target_key_arn
  filename         = data.archive_file.regression_wrapper_src.output_path
  source_code_hash = data.archive_file.regression_wrapper_src.output_base64sha256
  architectures    = ["arm64"]
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python313-arm64:7"]
  handler          = "${local.rw_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.13"
  timeout          = 360

  tags = {
    Name = local.rw_lambda_full_name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT  = local.env
      LOCUST_HOST      = "https://${aws_lb.this.dns_name}:${aws_lb_listener.this[local.green_state].port}"
      INVOKE_SQS_QUEUE = data.aws_sqs_queue.regression_invoke.url
      RESULT_SQS_QUEUE = data.aws_sqs_queue.regression_result.url
    }
  }

  role = aws_iam_role.regression_wrapper.arn
}

resource "aws_lambda_function_event_invoke_config" "regression_wrapper" {
  function_name          = aws_lambda_function.regression_wrapper.function_name
  maximum_retry_attempts = 0
}
