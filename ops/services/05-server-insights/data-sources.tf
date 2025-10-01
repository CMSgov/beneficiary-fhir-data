data "aws_ssm_parameter" "bfd_insights_bucket" {
  name = "/bfd/${local.env}/${local.target_service}/nonsensitive/bucket"
}

data "aws_s3_bucket" "bfd_insights_bucket" {
  bucket = nonsensitive(data.aws_ssm_parameter.bfd_insights_bucket.value)
}

data "aws_kms_key" "kms_key" {
  key_id = local.env_key_alias
}
