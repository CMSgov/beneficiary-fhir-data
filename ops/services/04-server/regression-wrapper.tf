locals {
  regression_wrapper_enabled = !local.is_ephemeral_env || var.ephemeral_regression_override

  rw_lambda_name      = "regression-wrapper"
  rw_lambda_full_name = "${local.name_prefix}-${local.rw_lambda_name}"
  rw_lambda_src       = replace(local.rw_lambda_name, "-", "_")
}

# We need to look at the network interfaces created by the Load Balancer to extract the private IP
# address so that the run-locust Lambda is able to connect to public LBs (like the prod-sbx/sandbox
# LB)
data "aws_network_interfaces" "load_balancer" {
  depends_on = [aws_lb.this]

  filter {
    name   = "description"
    values = ["ELB ${aws_lb.this.arn_suffix}"]
  }
}

data "aws_network_interface" "load_balancer" {
  id = sort(data.aws_network_interfaces.load_balancer.ids)[0]
}

data "aws_lambda_function" "run_locust" {
  count = local.regression_wrapper_enabled ? 1 : 0

  function_name = "bfd-${local.env}-locust-run-locust"
}

data "archive_file" "regression_wrapper_src" {
  count = local.regression_wrapper_enabled ? 1 : 0

  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.rw_lambda_name}/${local.rw_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.rw_lambda_name}/${local.rw_lambda_src}.py")
    filename = "${local.rw_lambda_src}.py"
  }
}

resource "aws_cloudwatch_log_group" "regression_wrapper" {
  count = local.regression_wrapper_enabled ? 1 : 0

  name         = "/aws/lambda/${local.rw_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "regression_wrapper" {
  count = local.regression_wrapper_enabled ? 1 : 0

  function_name = local.rw_lambda_full_name

  description = join("", [
    "Invoked on the AfterAllowTestTraffic CodeDeploy Lifecycle Event. This Lambda is a wrapper ",
    "around the ${one(data.aws_lambda_function.run_locust[*].function_name)} Lambda to run the ",
    "Regression test suite on replacement tasks"
  ])

  kms_key_arn      = local.env_key_arn
  filename         = one(data.archive_file.regression_wrapper_src[*].output_path)
  source_code_hash = one(data.archive_file.regression_wrapper_src[*].output_base64sha256)
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
      LOCUST_HOST            = "https://${data.aws_network_interface.load_balancer.private_ip}:${aws_lb_listener.this[local.green_state].port}"
      RUN_LOCUST_LAMBDA_NAME = one(data.aws_lambda_function.run_locust[*].function_name)
    }
  }

  role = one(aws_iam_role.regression_wrapper[*].arn)
}

resource "aws_lambda_function_event_invoke_config" "regression_wrapper" {
  count = local.regression_wrapper_enabled ? 1 : 0

  function_name          = one(aws_lambda_function.regression_wrapper[*].function_name)
  maximum_retry_attempts = 0
}
