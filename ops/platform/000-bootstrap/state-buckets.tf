module "bucket_tf_state" {
  for_each = toset(local.state_variants)

  source = "../../terraform-modules/general/secure-bucket"

  bucket_name        = "bfd-${each.key}-tf-state"
  bucket_kms_key_arn = aws_kms_key.tf_state[each.key].arn
  force_destroy      = false

  tags = {
    Environment = each.key
    stack       = each.key
  }
}
