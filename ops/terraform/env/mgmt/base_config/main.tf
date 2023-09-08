locals {
  env        = "mgmt"
  kms_key_id = data.aws_kms_key.cmk.arn
}
