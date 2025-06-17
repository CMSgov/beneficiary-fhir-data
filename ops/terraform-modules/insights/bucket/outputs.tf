output "arn" {
  value = aws_s3_bucket.main.arn
}

output "id" {
  value = aws_s3_bucket.main.id
}

output "bucket_cmk" {
  value = aws_kms_key.main.key_id
}

output "bucket_cmk_arn" {
  value = aws_kms_key.main.arn
}

output "iam_full_policy_body" {
  value = aws_iam_policy.full.policy
}
