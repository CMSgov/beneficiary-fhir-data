output "name" {
  value = aws_elb.main.name
}

output "lb_config" {
  value = {name=aws_elb.main.name, ports=var.egress.ports, sg=aws_security_group.lb.id}
}
