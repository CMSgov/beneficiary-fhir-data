locals {
  cli_cluster_data           = jsondecode(data.external.aws_cli_ecs_cluster.result.cluster)
  cluster_capacity_providers = local.cli_cluster_data.capacityProviders
  cluster_default_strategies = [
    for strategy in local.cli_cluster_data.defaultCapacityProviderStrategy
    : {
      capacity_provider = strategy.capacityProvider
      weight            = strategy.weight
      base              = strategy.base
    }
  ]
  ssm_capacity_provider_strategies = [
    for strategy in [
      for provider in local.cluster_capacity_providers
      : {
        capacity_provider = provider
        weight            = nonsensitive(lookup(var.ssm_config, "/bfd/${var.service}/ecs/capacity_provider/${provider}/weight", 0))
        base              = nonsensitive(lookup(var.ssm_config, "/bfd/${var.service}/ecs/capacity_provider/${provider}/base", 0))
      }
    ] : strategy if strategy.weight > 0 || strategy.base > 0 # Filter out unused strategies
  ]
  capacity_provider_strategies = coalescelist(local.ssm_capacity_provider_strategies, local.cluster_default_strategies)
}

# Data lookup is technically unnecessary, but is here in order to fail-fast prior to passing cluster
# name to external data source
data "aws_ecs_cluster" "main" {
  cluster_name = var.cluster_name
}

# As of 06/2025, the AWS Terraform Provider does not have a data resource that returns the capacity
# providers configured on an ECS Cluster. So, we need to use the AWS CLI to pull them.
data "external" "aws_cli_ecs_cluster" {
  program = [
    "bash",
    "-c",
    <<-EOF
    aws ecs describe-clusters --clusters ${data.aws_ecs_cluster.main.cluster_name} \
      --query 'clusters[0]' |
      jq -c '{"cluster": (. | tostring)}'
    EOF
  ]
}
