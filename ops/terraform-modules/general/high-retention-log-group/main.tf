locals {
  is_prod = terraform.workspace == "prod"
}

resource "aws_cloudwatch_log_group" "this" {
  name              = var.name
  retention_in_days = var.log_retention_days
  kms_key_id        = var.kms_key_id
  tags              = var.tags
  skip_destroy      = local.is_prod ? true : coalesce(var.skip_destroy, true)

  lifecycle {
    prevent_destroy = local.is_prod ? true : coalesce(var.skip_destroy, true)
  }
}
