output "asg_ids" {
  value = [aws_autoscaling_group.main["even"].name, aws_autoscaling_group.main["odd"].name]
}
