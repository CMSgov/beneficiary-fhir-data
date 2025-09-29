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
  relative_module_root = "ops/services/04-npi-pipeline"
  subnet_layers        = ["private"]
}

locals {
  service = "npi-pipeline"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = module.terraservice.ssm_config
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  data_subnets             = module.terraservice.subnets_map["private"]

  name_prefix = "bfd-${local.env}-${local.service}"

  pipeline_repository_name = coalesce(var.pipeline_repository_override, "bfd-pipeline-app")
  pipeline_version         = coalesce(var.pipeline_version_override, local.bfd_version)

  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"
  rds_writer_az         = module.data_db_writer_instance.writer.availability_zone
  # ECS Fargate does not allow specifying the AZ, but it does allow for specifying the subnet. So,
  # we can control which AZ the pipeline service/task is placed into by filtering the list of
  # subnets by AZ
  writer_adjacent_subnets = [for subnet in local.data_subnets : subnet.id if subnet.availability_zone == local.rds_writer_az]

  npi_ssm_hierarchies = [
    "/bfd/${local.env}/${local.service}/sensitive/",
    "/bfd/${local.env}/${local.service}/nonsensitive/",
    "/bfd/${local.env}/common/nonsensitive/",
  ]
  npi_cpu                          = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/cpu"])
  npi_memory                       = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/memory"])
  npi_disk_size                    = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/disk_size"])
  npi_capacity_provider_strategies = module.data_strategies.strategies
  npi_loader_thread_multiple       = 3
}

resource "aws_cloudwatch_log_group" "messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_ecs_task_definition" "this" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.service.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = local.npi_cpu
  memory       = local.npi_memory

  ephemeral_storage {
    size_in_gib = local.npi_disk_size
  }

  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }

  volume {
    configure_at_launch = false
    name                = "tmp"
  }

  container_definitions = jsonencode(
    [
      {
        name      = local.service
        image     = data.aws_ecr_image.pipeline.image_uri
        essential = true
        cpu       = 0
        environment = [
          {
            name  = "TZ"
            value = "UTC"
          },
          {
            name = "JDK_JAVA_OPTIONS"
            value = join(" ", [
              "-XX:+UseContainerSupport",
              "-XX:MaxRAMPercentage=75.0",
              "-XX:+PreserveFramePointer",
              "-XX:+UseStringDeduplication"
            ])
          },
          {
            name  = "BFD_ENV_NAME"
            value = local.env
          },
          {
            # This parameter includes "RDA" as this job was historically part of the "RDA Pipeline".
            # Now that it is distinct, the name of this parameter is nonsensical.
            name  = "BFD_RDA_NPI_FDA_LOAD_JOB_ENABLED"
            value = "true"
          },
          # CCW Pipeline Job defaults to enabled, so we need to explicitly disable it.
          {
            name  = "BFD_CCW_JOB_ENABLED"
            value = "false"
          },
          # Unfortunately, because the "RDA Pipeline" is just a job implemented within the greater
          # BFD Pipeline applcation, it is subject to some now-defunct assumptions about the
          # Pipeline itself. One of these is that all Jobs are configured under a common "pipeline"
          # hierarchy, and so regardless of which Jobs are enabled the Pipeline expects some
          # Job-specific configuration to exist. This is one of those configuration values
          # TODO: Remove this when/if Pipeline application configuration loading is fixed
          {
            name  = "BFD_CCW_IDEMPOTENCY_ENABLED"
            value = "true"
          },
          {
            name  = "BFD_DB_URL"
            value = "jdbc:postgresql://${module.data_db_writer_instance.writer.endpoint}/fhirdb?logServerErrorDetail=false"
          },
          {
            name  = "BFD_LOADER_THREAD_COUNT"
            value = tostring(max(floor(local.npi_cpu / 1000), 1) * local.npi_loader_thread_multiple)
          },
          {
            name = "CONFIG_SETTINGS_JSON"
            value = jsonencode(
              {
                ssmHierarchies = local.npi_ssm_hierarchies
              }
            )
          },
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.messages.name
            awslogs-stream-prefix = "messages"
            awslogs-region        = local.region
            max-buffer-size       = "25m"
            mode                  = "non-blocking"
          }
        }
        stopTimeout = 120 # Allow enough time for Pipeline to gracefully stop on spot termination.
        mountPoints = [
          {
            containerPath = "/tmp"
            readOnly      = false
            sourceVolume  = "tmp"
          }
        ]
        readonlyRootFilesystem = true
        # Empty declarations reduce Terraform diff noise
        portMappings   = []
        systemControls = []
        volumesFrom    = []
    }]
  )
}

resource "aws_security_group" "this" {
  name                   = "${local.name_prefix}-sg"
  description            = "Allow ${local.service} egress anywhere"
  vpc_id                 = local.vpc.id
  tags                   = { Name = "${local.name_prefix}-sg" }
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_security_group_ingress_rule" "allow_db_access" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.this.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.env} ${local.service} ECS task containers access to the ${local.env} database"
}

resource "aws_scheduler_schedule_group" "this" {
  name = "${local.name_prefix}-schedules"
}

resource "aws_scheduler_schedule" "this" {
  depends_on = [aws_iam_role_policy_attachment.schedule]

  name       = "${local.name_prefix}-every-30-days"
  group_name = aws_scheduler_schedule_group.this.name

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression = "cron(0 0 5 * ? *)"

  target {
    arn      = data.aws_ecs_cluster.main.arn
    role_arn = aws_iam_role.schedule.arn

    ecs_parameters {
      task_definition_arn     = aws_ecs_task_definition.this.arn
      task_count              = 1
      group                   = local.service
      enable_ecs_managed_tags = true
      propagate_tags          = "TASK_DEFINITION"
      platform_version        = "LATEST"

      dynamic "capacity_provider_strategy" {
        for_each = local.npi_capacity_provider_strategies
        content {
          capacity_provider = capacity_provider_strategy.value.capacity_provider
          base              = capacity_provider_strategy.value.base
          weight            = capacity_provider_strategy.value.weight
        }
      }

      network_configuration {
        assign_public_ip = false
        security_groups  = [aws_security_group.this.id]
        subnets          = local.writer_adjacent_subnets
      }
    }
  }
}
