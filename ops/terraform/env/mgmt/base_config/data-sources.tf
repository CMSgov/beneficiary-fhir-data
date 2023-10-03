data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}
