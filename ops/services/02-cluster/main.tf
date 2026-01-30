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
  account_id       = module.terraservice.account_id

  # Local module definitions
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  # Hashmap of EventBridge Names in the environments
  eventbridge_name = {
    prod = "EventsToLogs-bfd-pr-GkfqaQcy1BbcXQLjkTL6jeX8TnW71BHedM937r6P1Jap"
    sandbox = "EventsToLogs-bfd-sa-BnbucgSa4TiWrRdgEehNDermpJov5j1yJTyjfH1eDXbv"
    test = ""
  }
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

resource "aws_cloudwatch_event_rule" "ecs_events" {
  count = local.is_ephemeral_env ? 0 : 1

  name        = "${local.full_name}-ecs-cluster-events"
  description = "Monitor ECS cluster events."

  event_pattern = jsonencode({
    source = ["aws.ecs"]
    detail = {
      clusterArn = ["${aws_ecs_cluster.this.arn}"]
    }
  })
}

resource "aws_cloudwatch_event_target" "ecs_events_to_cloudwatch" {
  count = local.is_ephemeral_env ? 0 : 1

  depends_on = [
    aws_cloudwatch_log_resource_policy.eventbridge_to_logs
  ]

  rule = aws_cloudwatch_event_rule.ecs_events[0].name
  arn  = aws_cloudwatch_log_group.ecs_events[0].arn
}

resource "aws_cloudwatch_log_group" "ecs_events" {
  count = local.is_ephemeral_env ? 0 : 1

  name              = "/aws/ecs/containerinsights/${aws_ecs_cluster.this.name}/performance"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_resource_policy" "eventbridge_to_logs" {
  count = local.is_ephemeral_env ? 0 : 1

  policy_name     = "${local.full_name}-eventbridge-to-cloudwatch-logs"
  policy_document = data.aws_iam_policy_document.eventbridge_logs[0].json
}
