data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-mgmt-cmk"
}
