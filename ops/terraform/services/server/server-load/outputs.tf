output "controller_ip" {
  description = "For development purposes. When present, displays the locust 'controller' private IP address."
  value       = var.create_locust_instance ? aws_instance.this[0].private_ip : "no locust instance"
}
