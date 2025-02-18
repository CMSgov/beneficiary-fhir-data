output "legacy_clb_name" {
  value = aws_elb.main.name
}

output "legacy_sg_id" {
  value = aws_security_group.lb.id
}
