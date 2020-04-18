output "arn" {
  value = aws_s3_bucket.main.arn
}

output "id" {
  value = aws_s3_bucket.main.id
}

output "full_arn" {
  value = aws_iam_policy.full.arn
}

output "athena_query_arn" {
  value = aws_iam_policy.athena_query.arn
}

output "read_arn" {
  value = aws_iam_policy.read.arn
}
