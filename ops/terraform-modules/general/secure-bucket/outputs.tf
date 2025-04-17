output "bucket" {
  value       = aws_s3_bucket.this
  description = "`aws_s3_bucket` resource created by this module."
}
