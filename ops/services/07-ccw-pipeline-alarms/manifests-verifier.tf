locals {
  verifier_repository_default = "bfd-platform-pipeline-ccw-manifests-verifier-lambda"
  verifier_repository_name    = coalesce(var.manifests_verifier_repository_override, local.verifier_repository_default)
  verifier_version            = coalesce(var.manifests_verifier_version_override, local.bfd_version)
  verifier_alert_topics_ssm   = nonsensitive(lookup(local.ssm_config, "/bfd/${local.service}/verifier/alert_topics", null))
  verifier_alert_topics       = local.verifier_alert_topics_ssm != null ? [for v in split(",", local.verifier_alert_topics_ssm) : trimspace(v)] : null
  verifier_lambda_name        = "manifests-verifier"
  verifier_lambda_full_name   = "${local.name_prefix}-${local.verifier_lambda_name}"
}

data "aws_sns_topic" "verifier_alert_topic" {
  count = local.verifier_alert_topics != null ? length(local.verifier_alert_topics) : 0
  name  = local.verifier_alert_topics[count.index]
}

data "aws_ecr_image" "verifier" {
  repository_name = local.verifier_repository_name
  image_tag       = local.verifier_version
}

data "aws_rds_cluster" "main" {
  cluster_identifier = "bfd-${local.env}-aurora-cluster"
}

data "aws_ssm_parameter" "ccw_pipeline_bucket_name" {
  name = "/bfd/${local.env}/${local.target_service}/nonsensitive/bucket"
}

data "aws_s3_bucket" "ccw_pipeline" {
  bucket = nonsensitive(data.aws_ssm_parameter.ccw_pipeline_bucket_name.value)
}

resource "aws_scheduler_schedule_group" "verifier" {
  name = "${local.verifier_lambda_full_name}-schedules"
}

resource "aws_scheduler_schedule" "verifier" {
  name       = "${local.verifier_lambda_full_name}-mon-9am-et"
  group_name = aws_scheduler_schedule_group.verifier.name

  flexible_time_window {
    mode = "OFF"
  }

  # Schedule specifies to run every Monday at 9 AM ET
  schedule_expression          = "cron(0 9 ? * MON *)"
  schedule_expression_timezone = "America/New_York"

  target {
    arn      = aws_lambda_function.verifier.arn
    role_arn = aws_iam_role.verifier_scheduler.arn
  }
}

resource "aws_cloudwatch_log_group" "verifier" {
  name         = "/aws/lambda/${local.verifier_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "verifier" {
  depends_on = [aws_iam_role_policy_attachment.verifier]

  function_name = local.verifier_lambda_full_name

  description = join("", [
    "Lambda that reconciles the state of manifest files in S3 in the Incoming paths against their ",
    "state in the database verifying that they have been loaded. If any are unloaded, an alert is ",
    "sent"
  ])

  kms_key_arn      = local.env_key_arn
  image_uri        = data.aws_ecr_image.verifier.image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.verifier.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"
  memory_size      = 128
  timeout          = 60

  tags = {
    Name = local.verifier_lambda_full_name
  }

  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.verifier.name
  }

  environment {
    variables = {
      BFD_ENVIRONMENT  = local.env
      DB_CLUSTER_NAME  = data.aws_rds_cluster.main.cluster_identifier
      ETL_BUCKET_ID    = data.aws_s3_bucket.ccw_pipeline.bucket
      ALERT_TOPIC_ARNS = join(",", data.aws_sns_topic.verifier_alert_topic[*].arn)
    }
  }

  role = aws_iam_role.verifier.arn

  vpc_config {
    security_group_ids = [aws_security_group.verifier.id]
    subnet_ids         = local.app_subnets[*].id
  }
  replace_security_groups_on_destroy = true
}

resource "aws_security_group" "verifier" {
  description            = "Allow egress from ${local.verifier_lambda_full_name} Lambda"
  name                   = "${local.verifier_lambda_full_name}-sg"
  tags                   = { Name = "${local.verifier_lambda_full_name}-sg" }
  vpc_id                 = local.vpc.id
  revoke_rules_on_delete = true
}

resource "aws_vpc_security_group_egress_rule" "verifier_allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.verifier.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

data "aws_security_group" "aurora_cluster" {
  filter {
    name   = "tag:Name"
    values = [data.aws_rds_cluster.main.cluster_identifier]
  }
  filter {
    name   = "vpc-id"
    values = [local.vpc.id]
  }
}

resource "aws_vpc_security_group_ingress_rule" "verifier_allow_db_access" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.verifier.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.verifier_lambda_full_name} access to the ${local.env} database"
}


