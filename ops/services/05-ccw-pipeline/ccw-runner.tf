locals {
  ccw_runner_repository_default = "bfd-platform-pipeline-ccw-runner"
  ccw_runner_repository_name    = coalesce(var.ccw_runner_repository_override, local.ccw_runner_repository_default)
  ccw_runner_version            = coalesce(var.ccw_runner_version_override, local.bfd_version)
  ccw_runner_lambda_name        = "ccw-runner"
  ccw_runner_lambda_full_name   = "${local.name_prefix}-${local.ccw_runner_lambda_name}"
  ccw_runner_schedule_expr      = nonsensitive(local.ssm_config["/bfd/${local.service}/runner_lambda/schedule_expression"])
}

data "aws_ecr_image" "ccw_runner" {
  repository_name = local.ccw_runner_repository_name
  image_tag       = local.ccw_runner_version
}

resource "aws_security_group" "ccw_runner" {
  description            = "${local.ccw_runner_lambda_full_name} Lambda security group in ${local.env}"
  name                   = "${local.ccw_runner_lambda_full_name}-sg"
  tags                   = { Name = "${local.ccw_runner_lambda_full_name}-sg" }
  vpc_id                 = local.vpc.id
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "ccw_runner_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.ccw_runner.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_security_group_ingress_rule" "allow_ccw_runner_rds_cluster" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.ccw_runner.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.ccw_runner_lambda_full_name} Lambda access to the ${local.env} database"
}

resource "aws_cloudwatch_log_group" "ccw_runner" {
  name         = "/aws/lambda/${local.ccw_runner_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "ccw_runner" {
  depends_on = [aws_iam_role_policy_attachment.ccw_runner]

  function_name = local.ccw_runner_lambda_full_name
  description = join("", [
    "Lambda invoked by S3 Event Notifications and EventBridge Scheduler Schedules to run the CCW ",
    "Pipeline Task when manifests are available to load"
  ])
  tags = { Name = local.ccw_runner_lambda_full_name }

  kms_key_arn = local.env_key_arn

  image_uri        = data.aws_ecr_image.ccw_runner.image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.ccw_runner.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"

  memory_size = 128
  timeout     = 60

  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.ccw_runner.name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT                       = local.env
      DB_ENDPOINT                           = data.aws_rds_cluster.main.reader_endpoint
      CCW_BUCKET                            = module.bucket_ccw.bucket.bucket
      ECS_CLUSTER_ARN                       = data.aws_ecs_cluster.main.arn
      CCW_TASK_DEFINITION_ARN               = aws_ecs_task_definition.ccw.arn
      CCW_TASK_GROUP                        = local.service
      CCW_TASK_SUBNETS                      = join(",", local.writer_adjacent_subnets)
      CCW_TASK_SECURITY_GROUP_ID            = aws_security_group.ccw_runner.id
      CCW_TASK_TAGS_JSON                    = jsonencode(local.default_tags)
      CCW_TASK_CAPACITY_PROVIDER_STRATEGIES = jsonencode(local.ccw_capacity_provider_strategies)
    }
  }

  vpc_config {
    security_group_ids = [aws_security_group.ccw_runner.id]
    subnet_ids         = local.app_subnets[*].id
  }
  replace_security_groups_on_destroy = true

  role = aws_iam_role.ccw_runner.arn
}

resource "aws_scheduler_schedule_group" "ccw_runner" {
  name = "${aws_lambda_function.ccw_runner.function_name}-lambda-schedules"
}

resource "aws_scheduler_schedule" "ccw_runner" {
  name       = "${aws_lambda_function.ccw_runner.function_name}-schedule"
  group_name = aws_scheduler_schedule_group.ccw_runner.name

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression = local.ccw_runner_schedule_expr

  target {
    arn      = aws_lambda_function.ccw_runner.arn
    role_arn = aws_iam_role.ccw_runner_scheduler.arn
  }
}

resource "aws_s3_bucket_notification" "ccw_bucket_invoke_ccw_runner" {
  bucket = module.bucket_ccw.bucket.bucket

  dynamic "lambda_function" {
    for_each = {
      "Incoming"           = ["s3:ObjectCreated:*"]
      "Synthetic/Incoming" = ["s3:ObjectCreated:*"]
    }

    content {
      id            = "${aws_lambda_function.ccw_runner.function_name}-${lower(replace(lambda_function.key, "/", "-"))}"
      filter_prefix = "${lambda_function.key}/"
      # This is fine since the Lambda is also invoked on a rate schedule, so if the data load isn't
      # ready it will eventually be processed regardless
      filter_suffix       = "manifest.xml"
      events              = lambda_function.value
      lambda_function_arn = aws_lambda_function.ccw_runner.arn
    }
  }
}

resource "aws_lambda_permission" "ccw_runner_allow_s3" {
  statement_id   = "${aws_lambda_function.ccw_runner.function_name}-allow-s3"
  action         = "lambda:InvokeFunction"
  principal      = "s3.amazonaws.com"
  function_name  = aws_lambda_function.ccw_runner.function_name
  source_arn     = module.bucket_ccw.bucket.arn
  source_account = local.account_id
}


