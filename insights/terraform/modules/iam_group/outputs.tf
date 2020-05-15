output "arn" {
  value = aws_iam_group.main.arn
}

output "name" {
  value = local.full_name
}