module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  environment_name     = terraform.workspace
  service              = "migrator"
  relative_module_root = "ops/services/migrator"
  subnet_layers        = ["data"]
}

locals {
  service                  = module.terraservice.service
  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  seed_env                 = module.terraservice.seed_env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  latest_bfd_release       = module.terraservice.latest_bfd_release
  ssm_config               = module.terraservice.ssm_config
  env_key_alias            = module.terraservice.env_key_alias
  env_config_key_alias     = module.terraservice.env_config_key_alias
  env_key_arn              = module.terraservice.env_key_arn
  env_config_key_arns      = module.terraservice.env_config_key_arns
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  data_subnets             = module.terraservice.subnets_map["data"]

  name_prefix = "bfd-${local.env}-${local.service}"

  migrator_repository_name = coalesce(var.migrator_repository_override, "bfd-db-migrator")
  migrator_version         = coalesce(var.migrator_version_override, local.latest_bfd_release)

  db_environment        = coalesce(var.db_environment_override, local.env)
  db_cluster_identifier = "bfd-${local.db_environment}-aurora-cluster"
  rds_writer_az         = module.data_db_writer_instance.writer.availability_zone
  # ECS Fargate does not allow specifying the AZ, but it does allow for specifying the subnet. So,
  # we can control which AZ the migrator service/task is placed into by filtering the list of
  # subnets by AZ
  writer_adjacent_subnets = [for subnet in local.data_subnets : subnet.id if subnet.availability_zone == local.rds_writer_az]

  # TODO: Remove "/ng/" prefix when config is switched to
  ssm_hierarchies = [
    "/ng/bfd/${local.env}/${local.service}/sensitive/",
    "/ng/bfd/${local.env}/${local.service}/nonsensitive/",
    "/ng/bfd/${local.env}/common/nonsensitive/",
  ]
  cpu    = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/cpu"])
  memory = nonsensitive(local.ssm_config["/bfd/${local.service}/ecs/resources/memory"])
}

resource "aws_cloudwatch_log_group" "messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.service}/${local.service}/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

# TODO: Migrator should not run in Spot to avoid the possibility of Spot Termination
resource "aws_ecs_task_definition" "this" {
  family                   = local.name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = local.cpu
  memory       = local.memory

  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }

  container_definitions = jsonencode(
    [
      {
        name      = local.service
        image     = data.aws_ecr_image.migrator.image_uri
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
            name  = "BFD_DB_URL"
            value = "jdbc:postgresql://${module.data_db_writer_instance.writer.endpoint}/fhirdb?logServerErrorDetail=false"
          },
          {
            name = "CONFIG_SETTINGS_JSON"
            value = jsonencode(
              {
                ssmHierarchies = local.ssm_hierarchies
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
        # Empty declarations reduce Terraform diff noise
        mountPoints    = []
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

resource "null_resource" "start_migrator" {
  depends_on = [
    aws_iam_role.task,
    aws_iam_role.execution,
    aws_iam_role_policy_attachment.task,
    aws_iam_role_policy_attachment.execution
  ]

  triggers = {
    task_definition_revision = aws_ecs_task_definition.this.revision
  }

  provisioner "local-exec" {
    environment = {
      TASK_NAME           = local.service
      CONTAINER_NAME      = local.service
      CLUSTER_NAME        = data.aws_ecs_cluster.main.cluster_name
      TASK_DEFINITION_ARN = aws_ecs_task_definition.this.arn
      NETWORK_CONFIG_JSON = jsonencode({
        awsvpcConfiguration = {
          assignPublicIp = "DISABLED"
          securityGroups = [aws_security_group.this.id]
          subnets        = local.writer_adjacent_subnets
        }
      })
      TASK_TAGS_JSON = jsonencode([
        for key, value in local.default_tags
        : { key = key, value = tostring(value) }
      ])
      LOG_GROUP_NAME = aws_cloudwatch_log_group.messages.name
    }
    interpreter = ["/usr/bin/env", "bash"]
    quiet       = true
    command     = "${path.module}/scripts/run-migrator-task.sh"
  }
}
