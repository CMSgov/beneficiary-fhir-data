module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = "pipeline"
  relative_module_root = "ops/services/pipeline"
}

locals {
  service                  = module.terraservice.service
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

  name_prefix = "bfd-${local.env}-${local.service}"

  pipeline_repository_name = coalesce(var.pipeline_repository_override, "bfd-pipeline-app")
  pipeline_version         = coalesce(var.pipeline_version_override, local.latest_bfd_release)

  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"
  rds_writer_az         = data.aws_db_instance.writer.availability_zone
  # ECS Fargate does not allow specifying the AZ, but it does allow for specifying the subnet. So,
  # we can control which AZ the pipeline service/task is placed into by filtering the list of
  # subnets by AZ
  writer_adjacent_subnets = [for subnet in data.aws_subnet.data : subnet.id if subnet.availability_zone == local.rds_writer_az]

  thread_multiple = tonumber(nonsensitive(local.ssm_config["/bfd/${local.service}/thread_multiple"]))
}

data "aws_iam_policy_document" "ecs_tasks_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}
