output "endpoint" {
  value = aws_db_instance.db.endpoint
}

output "fqdn" {
  value = aws_route53_record.db.fqdn
}

output "identifier" {
  value = aws_db_instance.db.identifier
}
