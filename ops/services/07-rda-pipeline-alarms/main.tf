module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = local.service
  relative_module_root = "ops/services/07-rda-pipeline-alarms"
  subnet_layers        = ["app"]
}

locals {
  service = "rda-pipeline-alarms"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  seed_env                 = module.terraservice.seed_env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  latest_bfd_release       = module.terraservice.latest_bfd_release
  ssm_config               = module.terraservice.ssm_config
  env_key_alias            = module.terraservice.env_key_alias
  env_config_key_alias     = module.terraservice.env_config_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  env_config_key_arns      = module.terraservice.env_config_key_arns
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  app_subnets              = module.terraservice.subnets_map["app"]

  target_service = "rda-pipeline"
  name_prefix    = "bfd-${local.env}-${local.service}"

  metrics_namespace = "bfd-${local.env}/${local.target_service}"
  alarms_prefix     = "bfd-${local.env}-${local.target_service}"

  dashboard_url              = "https://${local.region}.console.aws.amazon.com/cloudwatch/home?region=${local.region}#dashboards:name=bfd-${local.env}-${local.target_service}"
  dashboard_message_fragment = <<-EOF
View the relevant CloudWatch dashboard below for more information:

* <${local.dashboard_url}|bfd-${local.env}-${local.target_service}>
    * This dashboard visualizes some important latency metrics
  EOF
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_cloudwatch_log_group" "messages" {
  name = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.target_service}/${local.target_service}/messages"
}
