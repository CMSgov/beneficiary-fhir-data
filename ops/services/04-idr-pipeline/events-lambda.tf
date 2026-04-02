locals {
  events_repository_default = "bfd-platform-consume-idr-events"
  events_repository_name    = coalesce(var.consume_idr_events_repository_override, local.events_repository_default)
  events_version            = coalesce(var.consume_idr_events_version_override, local.bfd_version)
  events_lambda_name        = "consume-idr-events"
  events_lambda_full_name   = "${local.name_prefix}-${local.events_lambda_name}"
  events_lambda_timeout     = aws_lambda_function.run_idr.timeout * 2
}

data "aws_ecr_image" "events" {
  repository_name = local.events_repository_name
  image_tag       = local.events_version
}

resource "aws_security_group" "events" {
  lifecycle {
    create_before_destroy = true
  }

  description            = "${local.events_lambda_full_name} Lambda security group in ${local.env}"
  name_prefix            = "${local.events_lambda_full_name}-sg"
  tags                   = { Name = "${local.events_lambda_full_name}-sg" }
  vpc_id                 = local.vpc.id
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "events_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.events.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_security_group_ingress_rule" "allow_events_rds_cluster" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.events.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.events_lambda_full_name} Lambda access to the ${local.env} database"
}

resource "aws_cloudwatch_log_group" "events" {
  name              = "/aws/lambda/${local.events_lambda_full_name}"
  kms_key_id        = local.env_key_arn
  retention_in_days = 30
  skip_destroy      = true
}

resource "aws_lambda_function" "events" {
  depends_on = [aws_iam_role_policy_attachment.events]

  function_name = local.events_lambda_full_name
  description = join("", [
    "Lambda invoked by ${aws_sqs_queue.events.name} SQS Queue to consume and load IDR Job events ",
    "into the v3 database"
  ])
  tags = {
    Name    = local.events_lambda_full_name,
    version = local.events_version
  }

  kms_key_arn = local.env_key_arn

  image_uri        = data.aws_ecr_image.events.image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.events.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"

  memory_size = 128
  timeout     = local.events_lambda_timeout

  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.events.name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT = local.env
      DB_ENDPOINT     = data.aws_rds_cluster.main.endpoint
    }
  }

  vpc_config {
    security_group_ids = [aws_security_group.events.id]
    subnet_ids         = local.app_subnets[*].id
  }
  replace_security_groups_on_destroy = true

  role = aws_iam_role.events.arn
}

resource "aws_lambda_permission" "events_allow_sqs" {
  statement_id   = "${aws_lambda_function.events.function_name}-allow-sqs"
  action         = "lambda:InvokeFunction"
  principal      = "sqs.amazonaws.com"
  function_name  = aws_lambda_function.events.function_name
  source_arn     = aws_sqs_queue.events.arn
  source_account = local.account_id
}

resource "aws_lambda_event_source_mapping" "events" {
  depends_on = [aws_iam_role_policy_attachment.events]

  event_source_arn = aws_sqs_queue.events.arn
  function_name    = aws_lambda_function.events.function_name
  batch_size       = 1
}

resource "aws_lambda_function_event_invoke_config" "events" {
  function_name          = aws_lambda_function.events.function_name
  maximum_retry_attempts = 2

  # If the Lambda exhausts all of its retry attempts, we want failing events to land into a DLQ such
  # that responding engineers can analyze the event and retry, if necessary
  destination_config {
    on_failure {
      destination = aws_sqs_queue.events_dlq.arn
    }
  }
}
