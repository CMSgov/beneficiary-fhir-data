
resource "aws_ecs_task_definition" "adot_collector" {
  family                   = "${local.full_name}-adot-collector"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512

  execution_role_arn = aws_iam_role.adot_collector_role.arn
  task_role_arn      = aws_iam_role.adot_collector_role.arn

  container_definitions = jsonencode([
    {
      name  = "adot-collector"
      image = "public.ecr.aws/aws-otel/aws-collector:latest"

      portMappings = [{
        containerPort = 4317
        protocol      = "tcp"
      }]

      environment = [
        {
          name  = "AWS_REGION"
          value = "${local.region}"
        },
        {
          name  = "OTEL_EXPORTER_OTLP_ENDPOINT"
          value = "https://localhost:4317"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs_group         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/otel/messages"
          awslogs_region        = "${local.region}"
          awslogs_stream_prefix = "collector"
        }
      }
    }
  ])
}

resource "aws_security_group" "adot" {
  lifecycle {
    create_before_destroy = true
  }
  name_prefix            = "${local.name_prefix}-sg"
  description            = "Allow ${local.service} egress anywhere"
  vpc_id                 = local.vpc.id
  tags                   = { Name = "${local.name_prefix}-sg" }
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "adot_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.adot.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_ecs_service" "adot_collector_service" {
  name            = "${local.full_name}-adot-collector"
  cluster         = data.aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.adot_collector.arn
  desired_count = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = local.subnets[*].id
    security_groups = [aws_security_group.adot.id]
  }
}
