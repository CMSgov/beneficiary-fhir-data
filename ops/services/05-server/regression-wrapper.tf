locals {
  rw_lambda_name      = "regression-wrapper"
  rw_lambda_full_name = "${local.name_prefix}-${local.rw_lambda_name}"
  rw_lambda_src       = replace(local.rw_lambda_name, "-", "_")
}

data "aws_lambda_function" "run_locust" {
  function_name = "bfd-${local.env}-locust-run-locust"
}

data "archive_file" "regression_wrapper_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.rw_lambda_name}/${local.rw_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.rw_lambda_name}/${local.rw_lambda_src}.py")
    filename = "${local.rw_lambda_src}.py"
  }
}

resource "aws_cloudwatch_log_group" "regression_wrapper" {
  name       = "/aws/lambda/${local.rw_lambda_full_name}"
  kms_key_id = local.env_key_arn
}

resource "aws_lambda_function" "regression_wrapper" {
  function_name = local.rw_lambda_full_name

  description = join("", [
    "Invoked on the AfterAllowTestTraffic CodeDeploy Lifecycle Event. This Lambda is a wrapper ",
    "around the existing Regression Suite Lambda to make it work with CodeDeploy"
  ])

  kms_key_arn      = local.env_key_arn
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
      BFD_ENVIRONMENT        = local.env
      LOCUST_HOST            = "https://${aws_lb.this.dns_name}:${aws_lb_listener.this[local.green_state].port}"
      RUN_LOCUST_LAMBDA_NAME = data.aws_lambda_function.run_locust.function_name
    }
  }

  role = aws_iam_role.regression_wrapper.arn
}

resource "aws_lambda_function_event_invoke_config" "regression_wrapper" {
  function_name          = aws_lambda_function.regression_wrapper.function_name
  maximum_retry_attempts = 0
}
