output "id" {
  value = aws_s3_bucket.main.id
}

output "arn" {
  value = aws_s3_bucket.main.arn
}

output "endpoint" {
  value = aws_s3_bucket.main.bucket_domain_name
}
