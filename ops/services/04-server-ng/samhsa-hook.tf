locals {
  samhsa_hook_enabled = !local.is_ephemeral_env || var.ephemeral_samhsa_hook_override

  sh_lambda_name      = "samhsa-hook"
  sh_lambda_full_name = "${local.name_prefix}-${local.sh_lambda_name}"
  sh_lambda_src       = replace(local.sh_lambda_name, "-", "_")
}

data "archive_file" "samhsa_hook_src" {
  count = local.samhsa_hook_enabled ? 1 : 0

  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.sh_lambda_name}/${local.sh_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.sh_lambda_name}/${local.sh_lambda_src}.py")
    filename = "${local.sh_lambda_src}.py"
  }
}

module "log_group_samhsa_hook" {
  source = "../../terraform-modules/general/high-retention-log-group"
  count  = local.samhsa_hook_enabled ? 1 : 0

  name         = "/aws/lambda/${local.sh_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "samhsa_hook" {
  depends_on = [
    aws_iam_role_policy_attachment.samhsa_hook,
    aws_lambda_function.samhsa_regression,
  ]
  count = local.samhsa_hook_enabled ? 1 : 0

  function_name = local.sh_lambda_full_name

  description = join("", [
    "Invoked on the POST_TEST_TRAFFIC_SHIFT ECS Lifecycle Event. This Lambda is a wrapper ",
    "around the ${one(aws_lambda_function.samhsa_regression[*].function_name)} Lambda to run ",
    "the SAMHSA filtering regression suite on replacement tasks"
  ])

  kms_key_arn      = local.env_key_arn
  filename         = one(data.archive_file.samhsa_hook_src[*].output_path)
  source_code_hash = one(data.archive_file.samhsa_hook_src[*].output_base64sha256)
  architectures    = ["arm64"]
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python314-arm64:27"]
  handler          = "${local.sh_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.14"
  timeout          = 360

  tags = {
    Name = local.sh_lambda_full_name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT               = local.env
      HOSTNAME                      = "https://${data.aws_network_interface.load_balancer.private_ip}:${aws_lb_listener.this[local.green_state].port}"
      SAMHSA_REGRESSION_LAMBDA_NAME = one(aws_lambda_function.samhsa_regression[*].function_name)
      TABLESAMPLE                   = "10.0"
      LIMIT                         = "300"
      CONCURRENCY                   = "10"
    }
  }

  role = one(aws_iam_role.samhsa_hook[*].arn)
}

resource "aws_lambda_function_event_invoke_config" "samhsa_hook" {
  count = local.samhsa_hook_enabled ? 1 : 0

  function_name          = one(aws_lambda_function.samhsa_hook[*].function_name)
  maximum_retry_attempts = 0
}
