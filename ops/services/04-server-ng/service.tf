locals {
  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"

  log_router_repository_default = !var.greenfield ? "bfd-mgmt-server-fluent-bit" : "bfd-platform-server-fluent-bit"
  log_router_repository_name    = coalesce(var.log_router_repository_override, local.log_router_repository_default)
  server_repository_name        = coalesce(var.server_repository_override, "bfd-server-ng")
  log_router_version            = coalesce(var.log_router_version_override, local.bfd_version)
  server_version                = coalesce(var.server_version_override, local.bfd_version)

  server_ssm_hierarchies              = ["/bfd/${local.env}/${local.service}"]
  server_port                         = 8080
  server_min_capacity                 = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/capacity/min"])
  server_max_capacity                 = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/capacity/max"])
  server_capacity_provider_strategies = module.data_strategies.strategies
  server_cpu                          = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/cpu"])
  server_memory                       = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/memory"])
  server_protocol                     = "tcp"
  server_healthcheck_client_cert      = replace(urlencode(trimspace(nonsensitive(local.ssm_config["/bfd/${local.service}/test_client_cert"]))), "+", "%20")
  server_healthcheck_bene_id          = nonsensitive(local.ssm_config["/bfd/${local.service}/heathcheck/testing_bene_id"])
  server_healthcheck_uri              = "http://localhost:${local.server_port}/v3/fhir/metadata"
}

data "aws_rds_cluster" "main" {
  cluster_identifier = local.db_cluster_identifier
}

data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

module "data_strategies" {
  source = "../../terraform-modules/bfd/bfd-data-ecs-strategies"

  service      = local.service
  ssm_config   = local.ssm_config
  cluster_name = data.aws_ecs_cluster.main.cluster_name
}

data "aws_ecr_repository" "log_router" {
  name = local.log_router_repository_name
}

data "aws_ecr_image" "log_router" {
  repository_name = data.aws_ecr_repository.log_router.name
  image_tag       = local.log_router_version
}

data "aws_ecr_repository" "server" {
  name = local.server_repository_name
}

data "aws_ecr_image" "server" {
  repository_name = data.aws_ecr_repository.server.name
  image_tag       = local.server_version
}

resource "aws_cloudwatch_log_group" "log_router_messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/log_router/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_cloudwatch_log_group" "service_connect_messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/service-connect/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_cloudwatch_log_group" "server_messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_cloudwatch_log_group" "server_access" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/access"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
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
    name                = "tmp_log_router"
  }

  volume {
    configure_at_launch = false
    name                = "tmp_${local.service}"
  }

  container_definitions = jsonencode(
    [
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
        mountPoints = [
          {
            containerPath = "/tmp"
            readOnly      = false
            sourceVolume  = "tmp_log_router"
          }
        ]
        readonlyRootFilesystem = true
        # Empty declarations reduce Terraform diff noise
        portMappings   = []
        systemControls = []
        volumesFrom    = []
      },
      {
        name      = local.service
        image     = data.aws_ecr_image.server.image_uri
        essential = true
        cpu       = 0
        environment = [
          {
            name = "JDK_JAVA_OPTIONS"
            value = join(" ", [
              "-XX:+UseContainerSupport",
              "-XX:MaxRAMPercentage=75.0",
              "-XX:+PreserveFramePointer",
              "-Dnetworkaddress.cache.ttl=5",
              "-Dsun.net.inetaddr.ttl=0"
            ])
          },
          {
            name  = "BFD_ENV"
            value = local.env
          },
          {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "aws"
          },
          {
            name  = "TZ"
            value = "UTC"
          },
        ]
        healthCheck = {
          command     = ["CMD-SHELL", "curl --silent --output /dev/null --fail -H 'X-Amzn-Mtls-Clientcert: ${local.server_healthcheck_client_cert}' '${local.server_healthcheck_uri}' || exit 1"]
          interval    = 10
          timeout     = 10
          retries     = 6
          startPeriod = 25
        }
        logConfiguration = {
          logDriver = "awsfirelens"
          options = {
            "log-driver-buffer-limit" = "2097152"
          }
        }
        portMappings = [
          {
            containerPort = tonumber(local.server_port)
            hostPort      = tonumber(local.server_port)
            name          = "${local.service}-${local.server_port}-${local.server_protocol}"
            protocol      = "${local.server_protocol}"
          },
        ]
        stopTimeout = 120 # Allow enough time for server to gracefully stop on spot termination.
        mountPoints = [
          {
            containerPath = "/tmp"
            readOnly      = false
            sourceVolume  = "tmp_${local.service}"
          }
        ]
        readonlyRootFilesystem = true
        # Empty declarations reduce Terraform diff noise
        dependsOn      = []
        systemControls = []
        volumesFrom    = []
      },
    ]
  )
}

resource "aws_security_group" "server" {
  name        = "${local.name_prefix}-sg"
  description = "Allow igress from NLBs to ${local.service} ECS service containers; egress anywhere"
  vpc_id      = local.vpc.id
  tags        = { Name = "${local.name_prefix}-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "server_allow_tls_nlb" {
  for_each = local.listeners

  security_group_id            = aws_security_group.server.id
  referenced_security_group_id = aws_security_group.lb[each.key].id
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
      "bfd-${local.parent_env}-aurora-cluster"
    ])
  }
  filter {
    name   = "vpc-id"
    values = [local.vpc.id]
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

resource "aws_service_discovery_http_namespace" "server" {
  name   = "${local.env}.service-connect.cmscloud.internal"
  region = "us-east-1"
}

resource "aws_ecs_service" "server" {
  depends_on = [
    aws_iam_role_policy_attachment.service_role,
    aws_iam_role_policy_attachment.execution,
    aws_iam_role_policy_attachment.deploy,
    aws_iam_role_policy_attachment.service_connect
  ]

  cluster                            = data.aws_ecs_cluster.main.arn
  availability_zone_rebalancing      = "ENABLED"
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 100
  desired_count                      = local.server_min_capacity
  enable_ecs_managed_tags            = true
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

    advanced_configuration {
      role_arn                   = aws_iam_role.deploy.arn
      production_listener_rule   = data.external.lb_listener_default_rule[local.blue_state].result.arn
      test_listener_rule         = data.external.lb_listener_default_rule[local.green_state].result.arn
      alternate_target_group_arn = aws_lb_target_group.this[1].arn
    }
  }

  deployment_controller {
    type = "ECS"
  }

  deployment_configuration {
    strategy             = "BLUE_GREEN"
    bake_time_in_minutes = 0
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  sigint_rollback       = true
  wait_for_steady_state = true

  dynamic "capacity_provider_strategy" {
    for_each = local.server_capacity_provider_strategies
    content {
      capacity_provider = capacity_provider_strategy.value.capacity_provider
      base              = capacity_provider_strategy.value.base
      weight            = capacity_provider_strategy.value.weight
    }
  }

  network_configuration {
    assign_public_ip = false
    security_groups  = [aws_security_group.server.id]
    subnets          = local.app_subnet_ids
  }

  service_connect_configuration {
    enabled   = true
    namespace = aws_service_discovery_http_namespace.server.arn

    log_configuration {
      log_driver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.service_connect_messages.name
        "awslogs-region"        = local.region
        "awslogs-stream-prefix" = "${local.service}-${local.bfd_version}"
      }
    }

    service {
      discovery_name        = local.service
      port_name             = "${local.service}-${local.server_port}-tcp"
      ingress_port_override = 0

      client_alias {
        dns_name = "${local.service}.${aws_service_discovery_http_namespace.server.name}"
        port     = local.server_port
      }

      tls {
        kms_key  = local.env_key_arn
        role_arn = aws_iam_role.service_connect.arn

        issuer_cert_authority {
          aws_pca_authority_arn = data.aws_acmpca_certificate_authority.pace.arn
        }
      }
    }
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
