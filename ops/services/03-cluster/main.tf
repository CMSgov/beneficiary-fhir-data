module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  relative_module_root = "ops/services/cluster"
  service              = local.service

  additional_tags = {
    Layer = local.layer
    Name  = local.full_name
    role  = local.service
  }
}

locals {
  # Local module definitions
  service   = "cluster"
  layer     = "data"
  full_name = "bfd-${local.env}-${local.service}"

  # Derived Values
  ## Module Lookups
  default_tags     = module.terraservice.default_tags
  env              = module.terraservice.env
  is_ephemeral_env = module.terraservice.is_ephemeral_env
  kms_key_alias    = nonsensitive(module.terraservice.ssm_config["/bfd/common/kms_key_alias"])

  ## Data Source Lookups
  kms_data_key_arn = data.aws_kms_key.cmk.arn
}

resource "aws_cloudwatch_log_group" "this" {
  name         = "/aws/ecs/${local.full_name}"
  kms_key_id   = local.kms_data_key_arn
  skip_destroy = true
}

resource "aws_ecs_cluster" "this" {
  name = local.full_name

  # TODO: More fully address security concerns such as this in BFD-3945
  tags = {
    GuardDutyManaged = true
  }

  setting {
    name  = "containerInsights"
    value = !local.is_ephemeral_env || var.container_insights_enabled_ephemeral_override ? "enhanced" : "disabled"
  }

  # TODO: Introduce a `managed_storage_configuration` block for cluster-level encryption
  #       Without this block, encryption on storage defaults to an AWS-managed key.
  #       This requires adjustments to the `mgmt` module
  #       Address in BFD-3945
  configuration {
    execute_command_configuration {
      kms_key_id = local.kms_data_key_arn
      logging    = "OVERRIDE"

      log_configuration {
        cloud_watch_log_group_name = aws_cloudwatch_log_group.this.name
      }
    }
  }
}

# TODO: Consider a dynamic selection based on prod/non-prod with FARGATE and FARGATE_SPOT
# TODO: Formalize `default_capacity_provider_strategy`
#       Address in BFD-3945
resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name = aws_ecs_cluster.this.name

  capacity_providers = ["FARGATE_SPOT"]

  default_capacity_provider_strategy {
    base              = 1
    weight            = 100
    capacity_provider = "FARGATE_SPOT"
  }
}
