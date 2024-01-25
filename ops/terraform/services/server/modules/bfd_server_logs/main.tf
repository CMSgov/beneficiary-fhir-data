locals {
  env        = var.env
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_access_json" {
  name       = "/bfd/${local.env}/bfd-server/access.json"
  kms_key_id = local.kms_key_id
}

resource "aws_cloudwatch_log_group" "bfd_server_messages_json" {
  name       = "/bfd/${local.env}/bfd-server/messages.json"
  kms_key_id = local.kms_key_id
}

resource "aws_cloudwatch_log_group" "bfd_server_newrelic_agent" {
  name       = "/bfd/${local.env}/bfd-server/newrelic_agent.log"
  kms_key_id = local.kms_key_id
}

resource "aws_cloudwatch_log_group" "bfd_server_gc" {
  name       = "/bfd/${local.env}/bfd-server/gc.log"
  kms_key_id = local.kms_key_id
}
