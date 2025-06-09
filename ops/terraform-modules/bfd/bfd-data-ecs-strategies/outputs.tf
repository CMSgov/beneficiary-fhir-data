output "strategies" {
  value       = local.capacity_provider_strategies
  description = "List of objects each representing a valid, configured capacity provider strategy that can be used in `aws_ecs_service` resources or Task executions"
}
