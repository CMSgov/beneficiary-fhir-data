resource "aws_cloudwatch_log_group" "this" {
  name              = var.log_group_name
  retention_in_days = var.log_retention_days
  kms_key_id        = var.kms_key_id
  tags              = var.tags

  lifecycle {
    prevent_destroy = var.prevent_destroy
  }
}
