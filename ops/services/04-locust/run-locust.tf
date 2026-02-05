locals {
  run_locust_repository_default = "bfd-platform-run-locust"
  run_locust_repository_name    = coalesce(var.run_locust_repository_override, local.run_locust_repository_default)
  run_locust_version            = coalesce(var.run_locust_version_override, local.bfd_version)
  run_locust_lambda_name        = "run-locust"
  run_locust_lambda_full_name   = "${local.name_prefix}-${local.run_locust_lambda_name}"
}

data "aws_rds_cluster" "main" {
  cluster_identifier = "bfd-${local.env}-aurora-cluster"
}

data "aws_ecr_image" "run_locust" {
  repository_name = local.run_locust_repository_name
  image_tag       = local.run_locust_version
}

resource "aws_security_group" "run_locust" {
  description = "${local.run_locust_lambda_full_name} Lambda security group in ${local.env}"
  name        = "${local.run_locust_lambda_full_name}-sg"
  tags        = { Name = "${local.run_locust_lambda_full_name}-sg" }
  vpc_id      = local.vpc.id

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
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

resource "aws_vpc_security_group_ingress_rule" "allow_run_locust" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.run_locust.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants ${local.run_locust_lambda_full_name} Lambda access to the ${local.env} database"
}

resource "aws_cloudwatch_log_group" "run_locust" {
  name         = "/aws/lambda/${local.run_locust_lambda_full_name}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "aws_lambda_function" "run_locust" {
  depends_on = [aws_iam_role_policy_attachment.run_locust]

  function_name = local.run_locust_lambda_full_name
  description   = "Lambda to run the Locust regression suite against the ${local.env} BFD Server"
  tags          = { Name = local.run_locust_lambda_full_name }

  kms_key_arn = local.env_key_arn

  image_uri        = data.aws_ecr_image.run_locust.image_uri
  source_code_hash = trimprefix(data.aws_ecr_image.run_locust.id, "sha256:")
  architectures    = ["arm64"]
  package_type     = "Image"

  memory_size = 2048
  timeout     = 10 * 60 # 10 minutes
  environment {
    variables = {
      BFD_ENVIRONMENT        = local.env
      STATS_BUCKET_ID        = module.bucket_athena.bucket.id
      STATS_ATHENA_WORKGROUP = aws_athena_workgroup.locust_stats.name
      STATS_ATHENA_DATABASE  = local.locust_stats_db_name
      STATS_ATHENA_TABLE     = local.locust_stats_table
      READER_ENDPOINT        = data.aws_rds_cluster.main.reader_endpoint
    }
  }

  vpc_config {
    security_group_ids = [aws_security_group.run_locust.id]
    subnet_ids         = local.app_subnets[*].id
  }
  replace_security_groups_on_destroy = true

  role = aws_iam_role.run_locust.arn
}
