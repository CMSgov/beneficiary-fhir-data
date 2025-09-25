terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  relative_module_root = "ops/services/02-cluster"
  service              = local.service

  additional_tags = {
    Layer = local.layer
    Name  = local.full_name
    role  = local.service
  }
}

locals {
  # Derived Values
  ## Module Lookups
  service = "cluster"

  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  is_ephemeral_env = module.terraservice.is_ephemeral_env
  env_key_arn      = module.terraservice.env_key_arn

  # Local module definitions
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"
}

resource "aws_cloudwatch_log_group" "this" {
  name         = "/aws/ecs/${local.full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_ecs_cluster" "this" {
  name = local.full_name

  # This may or may not be necessary, but it does not hurt to include. We do not manage GuardDuty
  # Runtime Monitoring configuration, but we can at least tag this cluster as opting into Runtime
  # Monitoring.
  tags = {
    GuardDutyManaged = true
  }

  setting {
    name  = "containerInsights"
    value = !local.is_ephemeral_env || var.container_insights_enabled_ephemeral_override ? "enhanced" : "disabled"
  }

  configuration {
    managed_storage_configuration {
      fargate_ephemeral_storage_kms_key_id = local.env_key_arn
      kms_key_id                           = local.env_key_arn
    }

    execute_command_configuration {
      kms_key_id = local.env_key_arn
      logging    = "OVERRIDE"

      log_configuration {
        cloud_watch_log_group_name = aws_cloudwatch_log_group.this.name
      }
    }
  }
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name = aws_ecs_cluster.this.name

  capacity_providers = ["FARGATE_SPOT", "FARGATE"]

  default_capacity_provider_strategy {
    base              = 1
    weight            = 100
    capacity_provider = "FARGATE_SPOT"
  }
}
