module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = local.service
  relative_module_root = "ops/services/05-ccw-pipeline-metrics"
}

locals {
  service = "ccw-pipeline-metrics"

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
