data "aws_ssm_parameter" "bfd_insights_bucket" {
  count = !var.greenfield ? 0 : 1

  name = "/bfd/${local.env}/${local.target_service}/nonsensitive/bucket"
}

data "aws_s3_bucket" "bfd_insights_bucket" {
  bucket = !var.greenfield ? "bfd-insights-bfd-${local.account_id}" : nonsensitive(one(data.aws_ssm_parameter.bfd_insights_bucket[*].value))
}

data "aws_kms_key" "kms_key" {
  key_id = !var.greenfield ? "alias/bfd-insights-bfd-cmk" : local.env_key_alias
}
