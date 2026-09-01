terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.52.0" # TODO: Replace with "~> 6" when ECS is fixed
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  service              = local.service
  relative_module_root = "ops/services/04-idr-bfd-validator"
  subnet_layers        = ["private"]
}

locals {
  service          = "idr-bfd-validator"
  pipeline_service = "idr-pipeline"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = module.terraservice.ssm_config
  env_key_arn              = module.terraservice.env_key_arn
  platform_key_arn         = module.terraservice.platform_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  private_subnets          = module.terraservice.subnets_map["private"]

  name_prefix = "bfd-${local.env}-${local.service}"

  container_repository_name = coalesce(var.container_repository_override, "bfd-platform-${local.service}")
  container_version         = coalesce(var.container_version_override, local.bfd_version)

  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"

  # Unlike other services with ECS Tasks, this Service creates Tasks that are never going to need
  # different resource limits, so we can avoid creating invariant/extraneous SSM configuration for
  # such values
  cpu       = 2048
  memory    = 4096
  disk_size = 21
  task_ssm = {
    for k, v in {
      IDR_USERNAME     = "/bfd/${local.env}/${local.pipeline_service}/sensitive/idr_username"
      IDR_PRIVATE_KEY  = "/bfd/${local.env}/${local.pipeline_service}/sensitive/idr_private_key"
      IDR_ACCOUNT      = "/bfd/${local.env}/${local.pipeline_service}/sensitive/idr_account"
      IDR_WAREHOUSE    = "/bfd/${local.env}/${local.pipeline_service}/sensitive/idr_warehouse"
      IDR_DATABASE     = "/bfd/${local.env}/${local.pipeline_service}/sensitive/idr_database"
      IDR_EDP_DATABASE = "/bfd/${local.env}/${local.pipeline_service}/sensitive/idr_edp_database"
      BFD_DB_USERNAME  = "/bfd/${local.env}/${local.pipeline_service}/sensitive/db/username"
      BFD_DB_PASSWORD  = "/bfd/${local.env}/${local.pipeline_service}/sensitive/db/password"
    } : k => "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(v, "/")}"
  }
  task_tmp_dir = "/app/.tmp"
}

resource "aws_cloudwatch_log_group" "messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = !local.is_ephemeral_env
}

resource "aws_security_group" "this" {
  lifecycle {
    create_before_destroy = true
  }

  name_prefix            = "${local.name_prefix}-sg"
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

resource "aws_ecs_task_definition" "this" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = local.cpu
  memory       = local.memory

  ephemeral_storage {
    size_in_gib = local.disk_size
  }

  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }

  tags = {
    "${local.service}.version" = local.container_version
  }

  container_definitions = jsonencode(
    [
      {
        name      = local.service
        image     = data.aws_ecr_image.this.image_uri
        essential = true
        cpu       = 0
        secrets = [
          for k, v in local.task_ssm :
          {
            name      = k
            valueFrom = v
          }
        ]
        environment = [
          {
            name  = "TZ"
            value = "UTC"
          },
          {
            name  = "TMPDIR",
            value = local.task_tmp_dir
          },
          {
            name  = "BFD_ENV"
            value = local.env
          },
          {
            name  = "BFD_DB_ENDPOINT"
            value = data.aws_rds_cluster.main.reader_endpoint
          },
          {
            name  = "IDR_STRUCTURED_LOGS",
            value = "1"
          },
          # TODO: Unexclude these tables when they work correctly
          {
            name = "IDR_EXCLUDE_TABLES"
            value = join(",", [
              "idr.prior_auth",
              "idr.prior_auth_item",
              "idr.beneficiary_low_income_subsidy_cmbnd"
            ])
          },
          # TODO: Uncommment this after BFD-4796 resolves known inconsistencies
          # {
          #   name  = "ALERT_SNS_TOPIC_ARN",
          #   value = data.aws_sns_topic.slack.arn
          # }
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
        stopTimeout = 120
        linuxParameters = {
          tmpfs = [
            {
              containerPath = "/app/.cache"
              size          = min(max(128, floor(0.025 * local.memory)), 256) # Min 128 MiB/max 256 MiB
              mountOptions = [
                "uid=1001",
                "gid=1001"
              ]
            },
            {
              containerPath = local.task_tmp_dir
              size          = 32 # 32 MiB
              mountOptions = [
                "uid=1001",
                "gid=1001"
              ]
            }
          ]
        }
        mountPoints            = []
        readonlyRootFilesystem = true
        # Empty declarations reduce Terraform diff noise
        portMappings   = []
        systemControls = []
        volumesFrom    = []
      }
    ]
  )
}

resource "aws_scheduler_schedule_group" "this" {
  # Only run in prod, for now
  count = local.env == "prod" ? 1 : 0

  name = "${local.name_prefix}-schedules"
}

resource "aws_scheduler_schedule" "this" {
  # Only run in prod, for now
  count      = local.env == "prod" ? 1 : 0
  depends_on = [aws_iam_role_policy_attachment.schedule]

  name       = "${local.name_prefix}-every-1-hour"
  group_name = one(aws_scheduler_schedule_group.this[*].name)

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression = "cron(0 * ? * * *)"

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

      capacity_provider_strategy {
        capacity_provider = "FARGATE"
        base              = 0
        weight            = 100
      }

      network_configuration {
        assign_public_ip = false
        security_groups  = [aws_security_group.this.id]
        subnets          = local.private_subnets[*].id
      }
    }
  }
}
