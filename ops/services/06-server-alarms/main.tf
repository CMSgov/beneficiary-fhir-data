terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.9"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  greenfield           = var.greenfield
  service              = local.service
  relative_module_root = "ops/services/06-server-alarms"
}

locals {
  service = "server-alarms"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  latest_bfd_release       = module.terraservice.latest_bfd_release
  ssm_config               = module.terraservice.ssm_config
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn

  target_service = "server"
  # TODO: Remove ecs suffix part when server is fully migrated to Fargate
  namespace = "bfd-${local.env}/${local.target_service}/ecs"

  # TODO: Remove "-ecs" suffix when Fargate migration is completed
  slo_dashboard_url                  = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#dashboards:name=bfd-${local.env}-server-ecs-slos"
  default_dashboard_url              = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#dashboards:name=bfd-${local.env}-server-ecs"
  default_dashboard_message_fragment = <<-EOF
View the relevant CloudWatch dashboards below for more information:

* <${local.slo_dashboard_url}|bfd-${local.env}-server-ecs-slos>
    * This dashboard visualizes SLOs along with ${local.target_service} Task count and CPU/memory utilization
* <${local.default_dashboard_url}|bfd-${local.env}-server-ecs>
    * This dashboard visualizes data such as request count and latency per-endpoint and per-partner, and more
  EOF

  # TODO: Remove "-ecs" suffix when Fargate migration is completed
  alarm_name_prefix = "bfd-${local.env}-${local.target_service}-ecs"
}

data "aws_cloudwatch_log_group" "server_access" {
  name = "/aws/ecs/bfd-${local.env}-cluster/${local.target_service}/${local.target_service}/access"
}

data "aws_cloudwatch_log_group" "server_messages" {
  name = "/aws/ecs/bfd-${local.env}-cluster/${local.target_service}/${local.target_service}/messages"
}
