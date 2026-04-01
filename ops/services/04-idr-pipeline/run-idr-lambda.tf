locals {
  run_idr_repository_default = "bfd-platform-run-idr-pipeline"
  run_idr_repository_name    = coalesce(var.run_idr_pipeline_repository_override, local.run_idr_repository_default)
  run_idr_version            = coalesce(var.run_idr_pipeline_version_override, local.bfd_version)
  run_idr_lambda_name        = "run-idr-pipeline"
  run_idr_lambda_full_name   = "${local.name_prefix}-${local.run_idr_lambda_name}"
}

data "aws_ecr_image" "run_idr" {
  repository_name = local.run_idr_repository_name
  image_tag       = local.run_idr_version
}

resource "aws_cloudwatch_log_group" "run_idr" {
  name              = "/aws/lambda/${local.run_idr_lambda_full_name}"
  kms_key_id        = local.env_key_arn
  retention_in_days = 30
  skip_destroy      = true
}

resource "aws_scheduler_schedule_group" "run_idr" {
  name = "${local.run_idr_lambda_full_name}-schedules"
}

resource "aws_lambda_function" "run_idr" {
  depends_on = [aws_iam_role_policy_attachment.run_idr]

  function_name = local.run_idr_lambda_full_name
  description = join("", [
    "Lambda invoked by ${local.events_lambda_full_name} Lambda or manually to run the ",
    "${local.service} Task"
  ])
  tags = {
    Name    = local.run_idr_lambda_full_name,
    version = local.run_idr_version
  }

  kms_key_arn = local.env_key_arn

  image_uri        = data.aws_ecr_image.run_idr.image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.run_idr.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"

  memory_size = 128
  timeout     = 60

  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.run_idr.name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT                            = local.env
      ECS_CLUSTER_ARN                            = data.aws_ecs_cluster.main.arn
      IDR_TASK_DEFINITION_ARN                    = aws_ecs_task_definition.idr_new.arn
      IDR_CONTAINER_NAME                         = local.service
      IDR_TASK_GROUP                             = local.service
      IDR_TASK_SUBNET_IDS_JSON                   = jsonencode(local.writer_adjacent_subnets)
      IDR_TASK_SECURITY_GROUP_ID                 = aws_security_group.idr_new.id
      IDR_TASK_CAPACITY_PROVIDER_STRATEGIES_JSON = jsonencode(local.idr_capacity_provider_strategies)
      IDR_TASK_SCHEDULES_GROUP                   = aws_scheduler_schedule_group.run_idr.name
      IDR_TASK_SCHEDULER_ROLE_ARN                = aws_iam_role.run_idr_scheduler.arn
    }
  }

  role = aws_iam_role.run_idr.arn
}

resource "aws_scheduler_schedule" "run_idr" {
  count = !local.is_ephemeral_env ? 1 : 0

  name       = "${local.run_idr_lambda_full_name}-daily-schedule"
  group_name = aws_scheduler_schedule_group.run_idr.name

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression          = "cron(0 12 ? * * *)"
  schedule_expression_timezone = "UTC"

  target {
    arn      = aws_lambda_function.run_idr.arn
    role_arn = aws_iam_role.run_idr_scheduler.arn
  }
}

