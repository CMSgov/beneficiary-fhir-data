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
  relative_module_root = "ops/services/05-rda-pipeline"
  subnet_layers        = ["private"]
}

locals {
  service = "rda-pipeline"

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

  rda_ssm_hierarchies = [
    "/bfd/${local.env}/${local.service}/sensitive/",
    "/bfd/${local.env}/${local.service}/nonsensitive/",
    "/bfd/${local.env}/common/nonsensitive/",
  ]
  rda_cpu                          = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/cpu"])
  rda_memory                       = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/memory"])
  rda_disk_size                    = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/disk_size"])
  rda_capacity_provider_strategies = module.data_strategies.strategies
  rda_loader_thread_multiple       = 3
  rda_thread_multiple_claims       = 25
}

resource "aws_cloudwatch_log_group" "rda_messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

module "bucket_rda" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_prefix      = local.name_prefix
  force_destroy      = local.is_ephemeral_env

  ssm_param_name = "/bfd/${local.env}/${local.service}/nonsensitive/bucket"
}

resource "aws_ecs_task_definition" "rda" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.rda_execution.arn
  task_role_arn            = aws_iam_role.rda_service.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = local.rda_cpu
  memory       = local.rda_memory

  ephemeral_storage {
    size_in_gib = local.rda_disk_size
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
            name  = "BFD_RDA_JOB_ENABLED"
            value = "true"
          },
          {
            name  = "BFD_RDA_GRPC_INPROCESS_SERVER_S3_BUCKET"
            value = module.bucket_rda.bucket.bucket
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
            value = tostring(max(floor(local.rda_cpu / 1000), 1) * local.rda_loader_thread_multiple)
          },
          {
            name = "CONFIG_SETTINGS_JSON"
            value = jsonencode(
              {
                ssmHierarchies = local.rda_ssm_hierarchies
              }
            )
          },
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.rda_messages.name
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

resource "aws_security_group" "rda" {
  name                   = "${local.name_prefix}-sg"
  description            = "Allow ${local.service} egress anywhere"
  vpc_id                 = local.vpc.id
  tags                   = { Name = "${local.name_prefix}-sg" }
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "rda_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.rda.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_security_group_ingress_rule" "rda_allow_db_access" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.rda.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.env} ${local.service} ECS task containers access to the ${local.env} database"
}

resource "aws_ecs_service" "rda" {
  depends_on = [aws_iam_role_policy_attachment.rda_service, aws_iam_role_policy_attachment.rda_execution]

  cluster                            = data.aws_ecs_cluster.main.arn
  availability_zone_rebalancing      = "ENABLED"
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  desired_count                      = 1
  enable_ecs_managed_tags            = true
  health_check_grace_period_seconds  = 0
  name                               = local.service
  propagate_tags                     = "TASK_DEFINITION"
  scheduling_strategy                = "REPLICA"
  task_definition                    = aws_ecs_task_definition.rda.arn

  dynamic "capacity_provider_strategy" {
    for_each = local.rda_capacity_provider_strategies
    content {
      capacity_provider = capacity_provider_strategy.value.capacity_provider
      base              = capacity_provider_strategy.value.base
      weight            = capacity_provider_strategy.value.weight
    }
  }

  deployment_controller {
    type = "ECS"
  }

  sigint_rollback       = true
  wait_for_steady_state = true

  network_configuration {
    assign_public_ip = false
    security_groups  = [aws_security_group.rda.id]
    subnets          = local.writer_adjacent_subnets
  }
}
