output "log_group" {
  value       = aws_cloudwatch_log_group.this
  description = "`aws_cloudwatch_log_group` resource created by this module."
}

output "arn" {
  value       = aws_cloudwatch_log_group.this.arn
  description = "ARN of the CloudWatch log group created by this module."
}

output "name" {
  value       = aws_cloudwatch_log_group.this.name
  description = "Name of the CloudWatch log group created by this module."
}
