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

  service              = local.service
  relative_module_root = "ops/services/05-idr-pipeline-metrics"
}

locals {
  service = "idr-pipeline-metrics"

  default_tags = module.terraservice.default_tags
  env          = module.terraservice.env

  target_service    = "idr-pipeline"
  metrics_namespace = "bfd-${local.env}/${local.target_service}"
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_cloudwatch_log_group" "messages" {
  name = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.target_service}/${local.target_service}/messages"
}

resource "aws_cloudwatch_log_metric_filter" "error_count" {
  name = "${local.metrics_namespace}/messages/count/error"

  # IDR uses Loguru, which pads the log level to eight characters.
  pattern        = "\"| ERROR    |\""
  log_group_name = data.aws_cloudwatch_log_group.messages.name

  metric_transformation {
    name          = "messages/count/error"
    namespace     = local.metrics_namespace
    value         = "1"
    default_value = "0"
  }
}
