output "asg_id" {
  value = aws_autoscaling_group.main.name
}
output "asg_config" {
  value = var.asg_config
}