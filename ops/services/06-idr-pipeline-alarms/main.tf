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
  relative_module_root = "ops/services/06-idr-pipeline-alarms"
  subnet_layers        = ["private"]
}

locals {
  service = "idr-pipeline-alarms"

  default_tags = module.terraservice.default_tags
  env          = module.terraservice.env
  ssm_config   = module.terraservice.ssm_config

  target_service    = "idr-pipeline"
  metrics_namespace = "bfd-${local.env}/${local.target_service}"
  alarms_prefix     = "bfd-${local.env}-${local.target_service}"
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_cloudwatch_log_group" "messages" {
  name = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.target_service}/${local.target_service}/messages"
}
