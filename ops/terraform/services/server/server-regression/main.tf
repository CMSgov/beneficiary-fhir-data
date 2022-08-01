provider "aws" {
  region = "us-east-1"
}

locals {
  shared_tags = {
    Environment = local.env
    Layer       = local.layer
    Name        = "bfd-${local.env}-${local.service}"
    application = "bfd"
    business    = "oeda"
    role        = local.service
    stack       = local.env
  }

  account_id = data.aws_caller_identity.current.account_id
  env        = terraform.workspace
  layer      = "app"
  service    = "server-regression"

  vpc_name   = "bfd-${local.env}-vpc"
  queue_name = "bfd-${local.env}-${local.service}"

  docker_image_tag = coalesce(var.docker_image_tag_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag.value))

  docker_image_uri = "${data.aws_ecr_repository.ecr.repository_url}:${local.docker_image_tag}"

  lambda_timeout_seconds = 600
  kms_key_arn            = data.aws_kms_key.cmk.arn
  kms_key_id             = data.aws_kms_key.cmk.key_id
}

resource "aws_lambda_function" "this" {
  function_name = "bfd-${local.env}-${local.service}"
  description   = "Lambda to run the Locust regression suite against the ${local.env} BFD Server"
  tags          = local.shared_tags
  kms_key_arn   = local.kms_key_arn

  image_uri    = local.docker_image_uri
  package_type = "Image"

  memory_size = 2048
  timeout     = local.lambda_timeout_seconds
  environment {
    variables = {
      BFD_ENVIRONMENT = local.env
    }
  }

  role = aws_iam_role.this.arn
  vpc_config {
    security_group_ids = [aws_security_group.this.id]
    subnet_ids         = data.aws_subnets.main.ids
  }
}

resource "aws_lambda_event_source_mapping" "this" {
  event_source_arn = aws_sqs_queue.this.arn
  function_name    = aws_lambda_function.this.arn
}

resource "aws_sqs_queue" "this" {
  name                       = local.queue_name
  visibility_timeout_seconds = local.lambda_timeout_seconds
  kms_master_key_id          = local.kms_key_id
}
