output "role" {
  value = aws_iam_role.instance.name
}

output "profile" {
  value = aws_iam_instance_profile.instance.name
}
