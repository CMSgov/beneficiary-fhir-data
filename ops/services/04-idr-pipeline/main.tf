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
  relative_module_root = "ops/services/04-idr-pipeline"
  subnet_layers        = ["private"]
}

locals {
  service = "idr-pipeline"

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
  app_subnets              = module.terraservice.subnets_map["private"]
  data_subnets             = module.terraservice.subnets_map["private"]

  name_prefix = "bfd-${local.env}-${local.service}"

  pipeline_repository_name = coalesce(var.pipeline_repository_override, "bfd-platform-${local.service}")
  pipeline_version         = coalesce(var.pipeline_version_override, local.bfd_version)

  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"
  rds_writer_az         = module.data_db_writer_instance.writer.availability_zone
  # ECS Fargate does not allow specifying the AZ, but it does allow for specifying the subnet. So,
  # we can control which AZ the pipeline service/task is placed into by filtering the list of
  # subnets by AZ
  writer_adjacent_subnets = [for subnet in local.data_subnets : subnet.id if subnet.availability_zone == local.rds_writer_az]

  idr_ssm_hierarchies = [
    "/bfd/${local.env}/${local.service}/sensitive/",
    "/bfd/${local.env}/${local.service}/nonsensitive/",
    "/bfd/${local.env}/common/nonsensitive/",
  ]
  idr_cpu                          = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/cpu"])
  idr_memory                       = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/memory"])
  idr_disk_size                    = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/disk_size"])
  idr_capacity_provider_strategies = module.data_strategies.strategies
  idr_task_ssm = {
    for k, v in {
      IDR_USERNAME    = "/bfd/${local.env}/${local.service}/sensitive/idr_username"
      IDR_PRIVATE_KEY = "/bfd/${local.env}/${local.service}/sensitive/idr_private_key"
      IDR_ACCOUNT     = "/bfd/${local.env}/${local.service}/sensitive/idr_account"
      IDR_WAREHOUSE   = "/bfd/${local.env}/${local.service}/sensitive/idr_warehouse"
      IDR_DATABASE    = "/bfd/${local.env}/${local.service}/sensitive/idr_database"
      IDR_SCHEMA      = "/bfd/${local.env}/${local.service}/sensitive/idr_schema"
      BFD_DB_USERNAME = "/bfd/${local.env}/${local.service}/sensitive/db/username"
      BFD_DB_PASSWORD = "/bfd/${local.env}/${local.service}/sensitive/db/password"
    } : k => "arn:aws:ssm:${local.region}:${local.account_id}:parameter/${trim(v, "/")}"
  }
}

resource "aws_cloudwatch_log_group" "idr_messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_security_group" "idr" {
  lifecycle {
    create_before_destroy = true
  }

  name_prefix            = "${local.name_prefix}-sg"
  description            = "Allow ${local.service} egress anywhere"
  vpc_id                 = local.vpc.id
  tags                   = { Name = "${local.name_prefix}-sg" }
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "idr_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.idr.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_security_group_ingress_rule" "idr_allow_db_access" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.idr.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.env} ${local.service} ECS task containers access to the ${local.env} database"
}

resource "aws_ecs_task_definition" "idr" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.idr_execution.arn
  task_role_arn            = aws_iam_role.idr_task.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = local.idr_cpu
  memory       = local.idr_memory

  ephemeral_storage {
    size_in_gib = local.idr_disk_size
  }

  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }

  tags = {
    "${local.service}.version" = local.pipeline_version
  }

  container_definitions = jsonencode(
    [
      {
        name      = local.service
        image     = data.aws_ecr_image.pipeline.image_uri
        essential = true
        cpu       = 0
        secrets = [
          for k, v in local.idr_task_ssm :
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
            name  = "BFD_ENV"
            value = local.env
          },
          {
            name  = "BFD_DB_ENDPOINT"
            value = data.aws_rds_cluster.main.endpoint
          },
          {
            name  = "IDR_MIN_CLAIM_NCH_TRANSACTION_DATE"
            value = "2014-06-30"
          },
          {
            name  = "IDR_MIN_CLAIM_SS_TRANSACTION_DATE"
            value = "2021-03-01"
          },
          {
            name  = "IDR_LATEST_CLAIMS"
            value = "0"
          },
          {
            name  = "IDR_LOAD_MODE",
            value = "prod"
          },
          {
            name  = "IDR_LOAD_TYPE",
            value = "incremental"
          },
          {
            name  = "IDR_SOURCE",
            value = "snowflake"
          },
          {
            name  = "IDR_ENABLE_DATE_PARTITIONS"
            value = "0"
          },
          {
            name = "CONFIG_SETTINGS_JSON"
            value = jsonencode(
              {
                ssmHierarchies = local.idr_ssm_hierarchies
              }
            )
          },
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.idr_messages.name
            awslogs-stream-prefix = "messages"
            awslogs-region        = local.region
            max-buffer-size       = "25m"
            mode                  = "non-blocking"
          }
        }
        stopTimeout = 120 # Allow enough time for Pipeline to gracefully stop on spot termination.
        linuxParameters = {
          tmpfs = [
            {
              containerPath = "/app/.cache"
              size          = min(max(128, floor(0.025 * local.idr_memory)), 256) # Min 128 MiB/max 256 MiB
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
