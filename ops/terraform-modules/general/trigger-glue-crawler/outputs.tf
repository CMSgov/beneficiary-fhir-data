output "lambda" {
  description = "Resource object (aws_lambda_function) representing the trigger Lambda created by this module"
  value       = aws_lambda_function.this
}
