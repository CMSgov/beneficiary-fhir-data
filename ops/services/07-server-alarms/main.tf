module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = "server-alarms"
  relative_module_root = "ops/services/server-alarms"
}

locals {
  service            = module.terraservice.service
  region             = module.terraservice.region
  account_id         = module.terraservice.account_id
  default_tags       = module.terraservice.default_tags
  env                = module.terraservice.env
  seed_env           = module.terraservice.seed_env
  is_ephemeral_env   = module.terraservice.is_ephemeral_env
  latest_bfd_release = module.terraservice.latest_bfd_release
  ssm_config         = module.terraservice.ssm_config

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
