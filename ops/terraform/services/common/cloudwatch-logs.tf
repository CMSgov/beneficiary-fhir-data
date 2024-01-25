## CloudWatch Log Groups by environment

resource "aws_cloudwatch_log_group" "var_log_messages" {
  name       = "/bfd/${local.env}/var/log/messages"
  kms_key_id = data.aws_kms_key.cmk.arn
}

resource "aws_cloudwatch_log_group" "var_log_secure" {
  name       = "/bfd/${local.env}/var/log/secure"
  kms_key_id = data.aws_kms_key.cmk.arn
}
