locals {
  env                 = terraform.workspace
  service             = "pipeline"
  layer               = "app"
  service_name_prefix = "bfd-${local.env}-${local.service}"

  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
  vpc_id     = data.aws_vpc.main.id
  kms_key_id = data.aws_kms_key.cmk.arn
  kms_config_key_arns = flatten(
    [
      for v in data.aws_kms_key.config_cmk.multi_region_configuration :
      concat(v.primary_key[*].arn, v.replica_keys[*].arn)
    ]
  )

  lambda_name      = "ccw-manifests-verifier"
  lambda_full_name = "${local.service_name_prefix}-${local.lambda_name}"
  lambda_src       = replace(local.lambda_name, "-", "_")
  lambda_image_uri = "${data.aws_ecr_repository.ecr.repository_url}:${var.bfd_version}"

  alert_topics = [for v in split(",", nonsensitive(data.aws_ssm_parameter.alert_topics.value)) : trimspace(v)]
}


resource "aws_scheduler_schedule_group" "this" {
  name = "${local.lambda_full_name}-lambda-schedules"
}

resource "aws_scheduler_schedule" "this" {
  name       = "${local.lambda_full_name}-mon-9am-et"
  group_name = aws_scheduler_schedule_group.this.name

  flexible_time_window {
    mode = "OFF"
  }

  # Schedule specifies to run every Monday at 9 AM ET
  schedule_expression          = "cron(0 9 ? * MON *)"
  schedule_expression_timezone = "America/New_York"

  target {
    arn      = aws_lambda_function.this.arn
    role_arn = aws_iam_role.scheduler_assume_role.arn
  }
}

resource "aws_lambda_function" "this" {
  depends_on = [aws_iam_role_policy_attachment.this]

  function_name = local.lambda_full_name

  description = join("", [
    "Lambda that reconciles the state of manifest files in S3 in the Incoming paths against their ",
    "state in the database verifying that they have been loaded. If any are unloaded, an alert is ",
    "sent"
  ])

  kms_key_arn      = local.kms_key_id
  image_uri        = local.lambda_image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.this.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"
  memory_size      = 128
  timeout          = 60

  tags = {
    Name = local.lambda_full_name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT  = local.env
      DB_CLUSTER_NAME  = var.db_cluster_identifier
      ETL_BUCKET_ID    = var.etl_bucket_id
      ALERT_TOPIC_ARNS = join(",", data.aws_sns_topic.alert_topic[*].arn)
    }
  }

  role = aws_iam_role.this.arn

  vpc_config {
    security_group_ids = [aws_security_group.this.id]
    subnet_ids         = data.aws_subnets.main.ids
  }
}

resource "aws_security_group" "this" {
  description = "Allow egress from ${local.lambda_full_name} Lambda"
  name        = local.lambda_full_name
  tags        = { Name = local.lambda_full_name }
  vpc_id      = data.aws_vpc.main.id

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group_rule" "rds" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  description              = "Allow ${local.lambda_full_name} access to ${var.db_cluster_identifier}"
  security_group_id        = data.aws_security_group.rds.id
  source_security_group_id = aws_security_group.this.id
}
