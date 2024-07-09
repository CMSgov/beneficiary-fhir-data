resource "aws_kms_key" "cfbucket_kms_key" {
  description             = "KMS key for S3 bucket encryption"
  deletion_window_in_days = 30
  
  policy = data.aws_iam_policy_document.cfbucket_kms_key_policy.json
}

resource "aws_kms_alias" "cfbucket_kms_alias" {
  name          = local.env_kms_alias
  target_key_id = aws_kms_key.cfbucket_kms_key.id
}
