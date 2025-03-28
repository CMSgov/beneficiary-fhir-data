data "aws_kms_key" "cmk" {
  key_id = local.kms_key_alias
}
