locals {
  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"

  log_router_repository_default = !var.greenfield ? "bfd-mgmt-server-fluent-bit" : "bfd-platform-server-fluent-bit"
  log_router_repository_name    = coalesce(var.log_router_repository_override, local.log_router_repository_default)
  server_repository_name        = coalesce(var.server_repository_override, "bfd-server-ng")
  log_router_version            = coalesce(var.log_router_version_override, local.latest_bfd_release)
  server_version                = coalesce(var.server_version_override, local.latest_bfd_release)

  server_ssm_hierarchies              = ["/bfd/${local.env}/${local.service}"]
  server_port                         = 8080
  server_min_capacity                 = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/capacity/min"])
  server_max_capacity                 = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/capacity/max"])
  server_capacity_provider_strategies = module.data_strategies.strategies
  server_cpu                          = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/cpu"])
  server_memory                       = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/memory"])
  server_protocol                     = "tcp"
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

data "aws_ecr_image" "log_router" {
  repository_name = local.log_router_repository_name
  image_tag       = local.log_router_version
}

data "aws_ecr_image" "server" {
  repository_name = local.server_repository_name
  image_tag       = local.server_version
}

resource "aws_cloudwatch_log_group" "log_router_messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/log_router/messages"
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
    name                = "certstores"
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
          command     = ["CMD-SHELL", "curl --silent --output /dev/null --fail '${local.server_healthcheck_uri}' || exit 1"]
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
        portMappings = [
          {
            containerPort = tonumber(local.server_port)
            hostPort      = tonumber(local.server_port)
            name          = "${local.service}-${local.server_port}-${local.server_protocol}"
            protocol      = "${local.server_protocol}"
          },
        ]
        stopTimeout = 120 # Allow enough time for server to gracefully stop on spot termination.
        # Empty declarations reduce Terraform diff noise
        dependsOn      = []
        mountPoints    = []
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

resource "aws_ecs_service" "server" {
  lifecycle {
    ignore_changes = [task_definition, desired_count, load_balancer, capacity_provider_strategy]
  }

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
  }

  dynamic "capacity_provider_strategy" {
    for_each = local.server_capacity_provider_strategies
    content {
      capacity_provider = capacity_provider_strategy.value.capacity_provider
      base              = capacity_provider_strategy.value.base
      weight            = capacity_provider_strategy.value.weight
    }
  }

  deployment_controller {
    type = "CODE_DEPLOY"
  }

  network_configuration {
    assign_public_ip = false
    security_groups  = [aws_security_group.server.id]
    subnets          = local.app_subnet_ids
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
