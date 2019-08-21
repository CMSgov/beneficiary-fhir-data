output "name" {
  value = aws_lb.main.name
}

output "target_group" {
  value = aws_lb_target_group.main.name
}

output "lb_config" {
  value = {name=aws_lb.main.name, tg_arn=aws_lb_target_group.main.arn, port=var.egress_port}
}
