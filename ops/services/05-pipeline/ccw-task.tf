locals {
  ccw_task_name   = "${local.service}-ccw"
  ccw_name_prefix = "${local.name_prefix}-ccw"
  ccw_ssm_hierarchies = [
    "/bfd/${local.env}/${local.service}/sensitive/",
    "/bfd/${local.env}/${local.service}/nonsensitive/",
    "/bfd/${local.env}/common/nonsensitive/",
  ]
  ccw_cpu                    = nonsensitive(local.ssm_config["/bfd/${local.service}/ccw/resources/cpu"])
  ccw_memory                 = nonsensitive(local.ssm_config["/bfd/${local.service}/ccw/resources/memory"])
  ccw_disk_size              = nonsensitive(local.ssm_config["/bfd/${local.service}/ccw/resources/disk_size"])
  ccw_thread_multiple_claims = tonumber(nonsensitive(local.ssm_config["/bfd/${local.service}/ccw/rif_thread_multiple_claims"]))
}

resource "aws_cloudwatch_log_group" "ccw_messages" {
  name         = "/aws/ecs/${data.aws_ecs_cluster.main.cluster_name}/${local.ccw_task_name}/${local.service}/messages"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

module "bucket_ccw" {
  source = "../../terraform-modules/general/secure-bucket"

  bucket_kms_key_arn = local.env_key_arn
  bucket_name        = local.ccw_name_prefix
  force_destroy      = local.is_ephemeral_env
}

resource "aws_ecs_task_definition" "ccw" {
  family                   = local.ccw_name_prefix
  requires_compatibilities = ["FARGATE"]
  execution_role_arn       = aws_iam_role.ccw_execution.arn
  task_role_arn            = aws_iam_role.ccw_task.arn
  track_latest             = false

  network_mode = "awsvpc"
  cpu          = local.ccw_cpu
  memory       = local.ccw_memory

  ephemeral_storage {
    size_in_gib = local.ccw_disk_size
  }

  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
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
            name  = "BFD_CCW_JOB_ENABLED"
            value = "true"
          },
          {
            name  = "BFD_RDA_JOB_ENABLED"
            value = "false"
          },
          {
            name  = "BFD_DB_URL"
            value = "jdbc:postgresql://${data.aws_db_instance.writer.endpoint}/fhirdb?logServerErrorDetail=false"
          },
          {
            name  = "BFD_CCW_S3_BUCKET_NAME"
            value = module.bucket_ccw.bucket.bucket
          },
          {
            name  = "BFD_LOADER_THREAD_COUNT"
            value = tostring(max(floor(local.ccw_cpu / 1000), 1) * local.thread_multiple)
          },
          {
            name  = "BFD_CCW_JOB_CLAIMS_LOADER_THREAD_COUNT"
            value = tostring(max(floor(local.ccw_cpu / 1000), 1) * local.ccw_thread_multiple_claims)
          },
          {
            name = "CONFIG_SETTINGS_JSON"
            value = jsonencode(
              {
                ssmHierarchies = local.ccw_ssm_hierarchies
              }
            )
          },
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            awslogs-group         = aws_cloudwatch_log_group.ccw_messages.name
            awslogs-stream-prefix = "messages"
            awslogs-region        = local.region
            max-buffer-size       = "25m"
            mode                  = "non-blocking"
          }
        }
        stopTimeout = 120 # Allow enough time for Pipeline to gracefully stop on spot termination.
        # Empty declarations reduce Terraform diff noise
        mountPoints    = []
        portMappings   = []
        systemControls = []
        volumesFrom    = []
    }]
  )
}

resource "aws_security_group" "ccw" {
  name                   = "${local.ccw_name_prefix}-sg"
  description            = "Allow ${local.ccw_task_name} egress anywhere"
  vpc_id                 = data.aws_vpc.main.id
  tags                   = { Name = "${local.ccw_name_prefix}-sg" }
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "ccw_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.ccw.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_security_group_ingress_rule" "ccw_allow_db_access" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.ccw.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.env} ${local.ccw_task_name} ECS task containers access to the ${local.env} database"
}
