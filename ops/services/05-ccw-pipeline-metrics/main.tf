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

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/05-ccw-pipeline-metrics"
}

locals {
  service = "ccw-pipeline-metrics"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  ssm_config               = module.terraservice.ssm_config
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc

  target_service = "ccw-pipeline"
  name_prefix    = "bfd-${local.env}-${local.service}"

  metrics_namespace = "bfd-${local.env}/${local.target_service}"
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_cloudwatch_log_group" "messages" {
  name = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.target_service}/${local.target_service}/messages"
}

resource "aws_cloudwatch_log_metric_filter" "error_count" {
  name           = "${local.metrics_namespace}/messages/count/error"
  pattern        = "[datetime, env, java_thread, level = \"ERROR\", java_class, message]"
  log_group_name = data.aws_cloudwatch_log_group.messages.name

  metric_transformation {
    name          = "messages/count/error"
    namespace     = local.metrics_namespace
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_log_metric_filter" "dataset_failed_count" {
  name           = "${local.metrics_namespace}/messages/count/datasetfailed"
  pattern        = "[datetime, env, java_thread, level = \"ERROR\", java_class, message = \"*Data set failed with an unhandled error*\"]"
  log_group_name = data.aws_cloudwatch_log_group.messages.name

  metric_transformation {
    name          = "messages/count/datasetfailed"
    namespace     = local.metrics_namespace
    value         = "1"
    default_value = "0"
  }
}
