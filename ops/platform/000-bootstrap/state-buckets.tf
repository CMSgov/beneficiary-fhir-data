module "bucket_tf_state" {
  for_each = toset(local.envs)

  source = "../../terraform-modules/general/secure-bucket"

  bucket_name        = each.key == "platform" ? "bfd-${each.key}-${local.account_type}-tf-state" : "bfd-${each.key}-tf-state"
  bucket_kms_key_arn = aws_kms_key.primary[each.key].arn
  force_destroy      = false

  tags = {
    Environment = each.key
    stack       = each.key
  }
}
