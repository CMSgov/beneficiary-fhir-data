output "name" {
  value = aws_elb.main.name
}

output "lb_config" {
  value = { name = aws_elb.main.name, port = var.egress.port, sg = aws_security_group.lb.id }
}
