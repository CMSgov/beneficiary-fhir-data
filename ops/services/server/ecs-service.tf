locals {
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"
  server_port           = local.ssm_config["/bfd/${local.service}/service_port"]
  server_ssm_hierarchies = [
    "/bfd/${local.env}/${local.service}/sensitive/",
    "/bfd/${local.env}/${local.service}/nonsensitive/",
    "/bfd/${local.env}/common/nonsensitive/",
  ]
  server_protocol = "tcp"
}

data "aws_rds_cluster" "main" {
  cluster_identifier = local.db_cluster_identifier
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_ecr_image" "certstores" {
  repository_name = local.certstores_repository_name
  image_tag       = local.certstores_version
}

data "aws_ecr_image" "server" {
  repository_name = local.server_repository_name
  image_tag       = local.server_version
}

resource "aws_cloudwatch_log_group" "server_service" {
  name       = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/service/${local.service}"
  kms_key_id = data.aws_kms_alias.env_cmk.target_key_arn
}

resource "aws_ecs_task_definition" "server" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.execution_role.arn
  task_role_arn            = aws_iam_role.service_role.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = "16384"
  memory       = "32768"
  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }

  volume {
    configure_at_launch = false
    name                = "certstores"
  }

  container_definitions = jsonencode(
    [
      {
        cpu = 0
        environment = [
          {
            name  = "BUCKET"
            value = aws_s3_bucket.certstores.bucket
          },
          {
            name  = "OUTPUT_PATH"
            value = "/data"
          },
        ]
        essential = false
        image     = data.aws_ecr_image.certstores.image_uri
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.server_service.name
            awslogs-stream-prefix = "/"
            awslogs-region        = local.region
            max-buffer-size       = "25m"
            mode                  = "non-blocking"
          }
        }
        mountPoints = [
          {
            containerPath = "/data"
            readOnly      = false
            sourceVolume  = "certstores"
          },
        ]
        name = "certstores"
      },
      {
        cpu = 0
        dependsOn = [
          {
            condition     = "COMPLETE"
            containerName = "certstores"
          },
        ]
        environment = [
          {
            name  = "BFD_DB_URL"
            value = "jdbc:postgresql://${data.aws_rds_cluster.main.reader_endpoint}:5432/fhirdb?logServerErrorDetail=false"
          },
          {
            name  = "BFD_ENV_NAME"
            value = local.env
          },
          {
            name  = "BFD_PATHS_FILES_KEYSTORE"
            value = "/data/${local.keystore_filename}"
          },
          {
            name  = "BFD_PATHS_FILES_TRUSTSTORE"
            value = "/data/${local.truststore_filename}"
          },
          {
            name  = "BFD_PATHS_FILES_WAR"
            value = "/app/bfd-server-war-${local.server_version}.war"
          },
          {
            name  = "BFD_PORT"
            value = local.server_port
          },
          {
            name = "CONFIG_SETTINGS_JSON"
            value = jsonencode(
              {
                ssmHierarchies = local.server_ssm_hierarchies
              }
            )
          },
          {
            name  = "TZ"
            value = "UTC"
          },
        ]
        essential = true
        image     = data.aws_ecr_image.server.image_uri
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.server_service.name
            awslogs-stream-prefix = "/"
            awslogs-region        = local.region
            max-buffer-size       = "25m"
            mode                  = "non-blocking"
          }
        }
        mountPoints = [
          {
            containerPath = "/data"
            readOnly      = true
            sourceVolume  = "certstores"
          },
        ]
        name = local.service
        portMappings = [
          {
            containerPort = tonumber(local.server_port)
            hostPort      = tonumber(local.server_port)
            name          = "${local.service}-${local.server_port}-${local.server_protocol}"
            protocol      = "${local.server_protocol}"
          },
        ]
      },
    ]
  )
}

resource "aws_security_group" "server" {
  name        = "${local.name_prefix}-sg"
  description = "Allow internal ingress to ${local.env} ${local.service} ECS service containers; egress anywhere"
  vpc_id      = data.aws_vpc.main.id
  tags        = { Name = "${local.name_prefix}-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "server_allow_tls_vpn" {
  security_group_id = aws_security_group.server.id
  prefix_list_id    = data.aws_ec2_managed_prefix_list.vpn.id
  from_port         = local.server_port
  ip_protocol       = local.server_protocol
  to_port           = local.server_port
}

resource "aws_vpc_security_group_egress_rule" "server_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.server.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

data "aws_security_groups" "aurora_cluster" {
  filter {
    name = "tag:Name"
    values = toset([
      local.db_cluster_identifier,
      "bfd-${local.seed_env}-aurora-cluster"
    ])
  }
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }
}

resource "aws_vpc_security_group_ingress_rule" "server_allow_db_access" {
  for_each = toset(data.aws_security_groups.aurora_cluster.ids)

  security_group_id            = each.value
  referenced_security_group_id = aws_security_group.server.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.env} ${local.service} ECS service containers access to the ${local.env} database"
}

resource "aws_ecs_service" "server" {
  depends_on = [aws_s3_object.keystore, aws_s3_object.truststore]

  cluster                            = data.aws_ecs_cluster.main.arn
  availability_zone_rebalancing      = "ENABLED"
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  desired_count                      = 1
  enable_ecs_managed_tags            = true
  enable_execute_command             = true
  health_check_grace_period_seconds  = 0
  name                               = local.service
  platform_version                   = "LATEST"
  propagate_tags                     = "NONE"
  scheduling_strategy                = "REPLICA"
  task_definition                    = aws_ecs_task_definition.server.arn
  triggers                           = {}

  capacity_provider_strategy {
    base              = 1
    capacity_provider = "FARGATE_SPOT"
    weight            = 100
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  deployment_controller {
    type = "ECS"
  }

  network_configuration {
    assign_public_ip = false
    security_groups  = [aws_security_group.server.id]
    subnets          = [for _, subnet in data.aws_subnet.app_subnets : subnet.id]
  }
}
