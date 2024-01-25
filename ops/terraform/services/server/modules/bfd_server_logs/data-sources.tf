data "aws_kms_key" "master_key" {
  key_id = var.kms_key_alias
}
