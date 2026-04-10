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

resource "aws_s3_bucket_versioning" "this" {
  for_each = module.bucket_tf_state
  
  # Assumes the module outputs a 'bucket' object with an 'id' attribute
  bucket = each.value.bucket.id 
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  for_each = module.bucket_tf_state
  bucket   = each.value.bucket.id

  depends_on = [aws_s3_bucket_versioning.this]

  rule {
    id     = "${each.value.bucket.id}-versions-retained"
    status = "Enabled"

    noncurrent_version_expiration {
      noncurrent_days           = 30
      newer_noncurrent_versions = 3
    }
  }
}
