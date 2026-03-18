locals {
  particpating_partners = "bcda"

  lambda_full_name   = "${local.name_prefix}-function"
  lambda_source_path = "${path.module}/lambda_src/bene_prefs"
  lambda_dist_path   = "${local.lambda_source_path}/package.zip"
  lambda_runtime     = "python${trimspace(file("${local.lambda_source_path}/.python-version"))}"
}

resource "aws_cloudwatch_log_group" "this" {
  count = local.conditional_count

  name       = "/aws/lambda/${local.name_prefix}"
  kms_key_id = local.env_key_arn
}

resource "aws_security_group" "lambda" {
  count = local.conditional_count

  description = "${local.lambda_full_name} Lambda security group in ${local.env}"
  name        = local.lambda_full_name
  tags        = { Name = "${local.lambda_full_name}-sg" }
  vpc_id      = local.vpc.id

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

data "external" "package" {
  count = local.conditional_count

  program = [
    "bash",
    "-c",
    "${local.lambda_source_path}/package.sh",
    local.env
  ]
}

resource "aws_lambda_function" "this" {
  count      = local.conditional_count
  depends_on = [aws_iam_role_policy_attachment.lambda[0]]

  function_name    = local.lambda_full_name
  description      = "Lambda to run the ${local.service} service for DASG"
  filename         = local.lambda_dist_path
  kms_key_arn      = local.env_key_arn
  source_code_hash = data.external.package[0].result.hash
  architectures    = ["arm64"]
  package_type     = "Zip"
  runtime          = local.lambda_runtime
  handler          = "app.main.handler"
  memory_size      = 1024
  timeout          = 10 * 60 # 10 minutes

  tags = {
    Name   = local.lambda_full_name
    sha256 = data.external.package[0].result.hash
  }

  environment {
    variables = {
      BFD_ENV            = local.env
      AWS_CURRENT_REGION = local.region
      PARTNERS           = local.particpating_partners
    }
  }

  vpc_config {
    security_group_ids = [aws_security_group.lambda[0].id]
    subnet_ids         = local.app_subnets[*].id
  }
  replace_security_groups_on_destroy = true

  role = aws_iam_role.lambda[0].arn
}

resource "aws_cloudwatch_event_rule" "this" {
  count = local.conditional_count

  name                = local.lambda_full_name
  description         = "Trigger {aws_lambda_function.this[0].function_name}"
  schedule_expression = "cron(17 10,22 ? * MON-SAT *)"
}

resource "aws_cloudwatch_event_target" "this" {
  count = local.conditional_count

  arn  = aws_lambda_function.this[0].arn
  rule = aws_cloudwatch_event_rule.this[0].name
}

resource "aws_lambda_permission" "this" {
  count = local.conditional_count

  statement_id  = "AllowExecutionFromCloudWatchEvents"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.this[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.this[0].arn
}
