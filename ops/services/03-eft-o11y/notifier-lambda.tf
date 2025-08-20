locals {
  slack_notifier_lambda_name = "bfd-${local.env}-${local.service}-outbound-slack-notifier"
  slack_notifier_lambda_src  = "outbound_slack_notifier"
  slack_webhook_ssm_path     = local.ssm_config["/bfd/${local.service}/outbound/slack_notifier/webhook_ssm_path"]
  slack_webhook              = nonsensitive(data.aws_ssm_parameter.slack_webhook.value)
}

data "aws_ssm_parameter" "slack_webhook" {
  name            = local.slack_webhook_ssm_path
  with_decryption = true
}

resource "aws_lambda_permission" "slack_notifier_allow_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.slack_notifier.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = local.eft_outputs.outbound_bfd_sns_topic_arn
}

resource "aws_sns_topic_subscription" "sns_to_slack_notifier" {
  topic_arn = local.eft_outputs.outbound_bfd_sns_topic_arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.slack_notifier.arn
}

data "archive_file" "slack_notifier_src" {
  type        = "zip"
  output_path = "${path.module}/lambda_src/${local.slack_notifier_lambda_src}/${local.slack_notifier_lambda_src}.zip"

  source {
    content  = file("${path.module}/lambda_src/${local.slack_notifier_lambda_src}/${local.slack_notifier_lambda_src}.py")
    filename = "${local.slack_notifier_lambda_src}.py"
  }
}

resource "aws_lambda_function" "slack_notifier" {
  function_name = local.slack_notifier_lambda_name

  description = join("", [
    "Invoked when the ${local.eft_outputs.outbound_bfd_sns_topic_name} sends a notification. This Lambda posts ",
    "the contents of the notification to the configured Slack channel"
  ])

  kms_key_arn      = local.env_key_arn
  filename         = data.archive_file.slack_notifier_src.output_path
  source_code_hash = data.archive_file.slack_notifier_src.output_base64sha256
  architectures    = ["arm64"]
  layers           = ["arn:aws:lambda:${local.region}:017000801446:layer:AWSLambdaPowertoolsPythonV3-python313-arm64:18"]
  handler          = "${local.slack_notifier_lambda_src}.handler"
  memory_size      = 128
  package_type     = "Zip"
  runtime          = "python3.13"
  timeout          = 300

  tags = {
    Name = local.slack_notifier_lambda_name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT   = local.env
      SLACK_WEBHOOK_URL = local.slack_webhook
    }
  }

  role = one(aws_iam_role.slack_notifier[*].arn)
}
