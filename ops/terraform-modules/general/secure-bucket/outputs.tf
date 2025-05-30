output "bucket" {
  value       = aws_s3_bucket.this
  description = "`aws_s3_bucket` resource created by this module."
}

output "ssm_bucket_name" {
  value       = aws_ssm_parameter.bucket_name
  description = "`aws_ssm_parameter` resource created by this module. Is null if var.ssm_param_name is unspecified/null."
}
