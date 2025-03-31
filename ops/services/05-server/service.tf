locals {
  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"

  certstores_repository_name = coalesce(var.certstores_repository_override, "bfd-mgmt-mount-certstores")
  log_router_repository_name = coalesce(var.log_router_repository_override, "bfd-mgmt-server-fluent-bit")
  server_repository_name     = coalesce(var.server_repository_override, "bfd-server")
  certstores_version         = coalesce(var.certstores_version_override, local.latest_bfd_release)
  log_router_version         = coalesce(var.log_router_version_override, local.latest_bfd_release)
  server_version             = coalesce(var.server_version_override, local.latest_bfd_release)

  server_truststore_path = "/data/${local.truststore_filename}"
  server_keystore_path   = "/data/${local.keystore_filename}"
  server_port            = nonsensitive(local.ssm_config["/bfd/${local.service}/service_port"])
  server_min_capacity    = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/capacity/min"])
  server_max_capacity    = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/capacity/max"])
  server_cpu             = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/cpu"])
  server_memory          = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/memory"])
  server_ssm_hierarchies = [
    "/bfd/${local.env}/${local.service}/sensitive/",
    "/bfd/${local.env}/${local.service}/nonsensitive/",
    "/bfd/${local.env}/common/nonsensitive/",
  ]
  server_protocol             = "tcp"
  server_healthcheck_pem_path = "/data/healthcheck.pem"
  server_healthcheck_bene_id  = nonsensitive(local.ssm_config["/bfd/${local.service}/heathcheck/testing_bene_id"])
  server_healthcheck_uri      = "https://localhost:${local.server_port}/v2/fhir/ExplanationOfBenefit/?_format=application%2Ffhir%2Bjson&patient=${local.server_healthcheck_bene_id}"
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

data "aws_ecr_image" "log_router" {
  repository_name = local.log_router_repository_name
  image_tag       = local.log_router_version
}

data "aws_ecr_image" "server" {
  repository_name = local.server_repository_name
  image_tag       = local.server_version
}

resource "aws_cloudwatch_log_group" "certstores_messages" {
  name       = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/certstores/messages"
  kms_key_id = local.env_key_arn
}

resource "aws_cloudwatch_log_group" "log_router_messages" {
  name       = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/log_router/messages"
  kms_key_id = local.env_key_arn
}

resource "aws_cloudwatch_log_group" "server_messages" {
  name       = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/messages"
  kms_key_id = local.env_key_arn
}

resource "aws_cloudwatch_log_group" "server_access" {
  name       = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/access"
  kms_key_id = local.env_key_arn
}

resource "aws_ecs_task_definition" "server" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.execution_role.arn
  task_role_arn            = aws_iam_role.service_role.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = local.server_cpu
  memory       = local.server_memory
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
        name      = "certstores"
        image     = data.aws_ecr_image.certstores.image_uri
        essential = false
        cpu       = 0
        environment = [
          {
            name  = "BFD_ENV"
            value = local.env
          },
          {
            name  = "BUCKET"
            value = module.bucket_certstores.bucket.bucket
          },
          {
            name  = "TRUSTSTORE_KEY"
            value = local.truststore_s3_key
          },
          {
            name  = "KEYSTORE_KEY"
            value = local.keystore_s3_key
          },
          {
            name  = "TRUSTSTORE_OUTPUT_PATH"
            value = local.server_truststore_path
          },
          {
            name  = "KEYSTORE_OUTPUT_PATH"
            value = local.server_keystore_path
          },
          {
            name  = "HEALTHCHECK_CERT_OUTPUT_PATH"
            value = local.server_healthcheck_pem_path
          },
          {
            name  = "REGION"
            value = local.region
          }
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.certstores_messages.name
            awslogs-stream-prefix = "messages"
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
        # Empty declarations reduce Terraform diff noise
        portMappings   = []
        systemControls = []
        volumesFrom    = []
      },
      {
        name              = "log_router"
        image             = data.aws_ecr_image.log_router.image_uri
        essential         = true
        cpu               = 128
        memoryReservation = 50
        memory            = 100
        user              = "0" # Default; reduces unnecessary terraform diff output
        environment = [
          {
            name  = "AWS_REGION"
            value = local.region
          },
          {
            name  = "MESSAGES_LOG_GROUP"
            value = aws_cloudwatch_log_group.server_messages.name
          },
          {
            name  = "ACCESS_LOG_GROUP"
            value = aws_cloudwatch_log_group.server_access.name
          },
        ]
        firelensConfiguration = {
          type = "fluentbit"
          options = {
            "config-file-type"        = "file"
            "config-file-value"       = "/server-fluentbit.conf"
            "enable-ecs-log-metadata" = "false"
          }
        }
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.log_router_messages.name
            awslogs-stream-prefix = "messages"
            awslogs-region        = local.region
            max-buffer-size       = "25m"
            mode                  = "non-blocking"
          }
        }
        # Empty declarations reduce Terraform diff noise
        mountPoints    = []
        portMappings   = []
        systemControls = []
        volumesFrom    = []
      },
      {
        name      = local.service
        image     = data.aws_ecr_image.server.image_uri
        essential = true
        cpu       = 0
        dependsOn = [
          {
            condition     = "COMPLETE"
            containerName = "certstores"
          },
        ]
        environment = [
          {
            name = "JDK_JAVA_OPTIONS"
            value = join(" ", [
              "-Dlogback.configurationFile=all-stdout.logback.xml",
              "-XX:+UseContainerSupport",
              "-XX:MaxRAMPercentage=75.0",
              "-XX:+PreserveFramePointer",
              "-Dnetworkaddress.cache.ttl=5",
              "-Dsun.net.inetaddr.ttl=0"
            ])
          },
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
            value = local.server_keystore_path
          },
          {
            name  = "BFD_PATHS_FILES_TRUSTSTORE"
            value = local.server_truststore_path
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
        healthCheck = {
          command     = ["CMD-SHELL", "curl --silent --insecure --cert '${local.server_healthcheck_pem_path}' --output /dev/null --fail '${local.server_healthcheck_uri}' || exit 1"]
          interval    = 10
          timeout     = 10
          retries     = 6
          startPeriod = 60
        }
        logConfiguration = {
          logDriver = "awsfirelens"
          options = {
            "log-driver-buffer-limit" = "2097152"
          }
        }
        mountPoints = [
          {
            containerPath = "/data"
            readOnly      = true
            sourceVolume  = "certstores"
          },
        ]
        portMappings = [
          {
            containerPort = tonumber(local.server_port)
            hostPort      = tonumber(local.server_port)
            name          = "${local.service}-${local.server_port}-${local.server_protocol}"
            protocol      = "${local.server_protocol}"
          },
        ]
        # Empty declarations reduce Terraform diff noise
        systemControls = []
        volumesFrom    = []
      },
    ]
  )
}

resource "aws_security_group" "server" {
  name        = "${local.name_prefix}-sg"
  description = "Allow igress from NLBs to ${local.service} ECS service containers; egress anywhere"
  vpc_id      = data.aws_vpc.main.id
  tags        = { Name = "${local.name_prefix}-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "server_allow_tls_nlb" {
  for_each = aws_security_group.lb

  security_group_id            = aws_security_group.server.id
  referenced_security_group_id = each.value.id
  from_port                    = local.server_port
  ip_protocol                  = local.server_protocol
  to_port                      = local.server_port
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

  lifecycle {
    ignore_changes = [task_definition, desired_count, load_balancer, capacity_provider_strategy]
  }

  cluster                            = data.aws_ecs_cluster.main.arn
  availability_zone_rebalancing      = "ENABLED"
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  desired_count                      = local.server_min_capacity
  enable_ecs_managed_tags            = true
  enable_execute_command             = true
  health_check_grace_period_seconds  = 0
  name                               = local.service
  propagate_tags                     = "NONE"
  scheduling_strategy                = "REPLICA"
  task_definition                    = aws_ecs_task_definition.server.arn
  triggers                           = {}

  load_balancer {
    target_group_arn = aws_lb_target_group.this[0].arn
    container_name   = local.service
    container_port   = local.server_port
  }

  capacity_provider_strategy {
    base              = 1
    capacity_provider = "FARGATE_SPOT"
    weight            = 100
  }

  deployment_controller {
    type = "CODE_DEPLOY"
  }

  network_configuration {
    assign_public_ip = false
    security_groups  = [aws_security_group.server.id]
    subnets          = [for _, subnet in data.aws_subnet.app_subnets : subnet.id]
  }
}

resource "aws_appautoscaling_target" "server" {
  max_capacity       = local.server_max_capacity
  min_capacity       = local.server_min_capacity
  resource_id        = "service/${data.aws_ecs_cluster.main.cluster_name}/${aws_ecs_service.server.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "server_track_cpu" {
  name               = "${local.name_prefix}-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.server.resource_id
  scalable_dimension = aws_appautoscaling_target.server.scalable_dimension
  service_namespace  = aws_appautoscaling_target.server.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }

    target_value       = 50
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}
