provider "aws" {
  region = "us-east-1"
}

locals {
  common_tags = {
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
  service    = "locust-regression"

  vpc_name   = "bfd-${local.env}-vpc"
  queue_name = "bfd-${local.env}-${local.service}"

  docker_image_tag = coalesce(var.docker_image_tag_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag.value))

  docker_image_uri = "${data.aws_ecr_repository.ecr.repository_url}:${local.docker_image_tag}"
}

resource "aws_lambda_function" "this" {
  function_name = "bfd-${local.env}-${local.service}"
  description   = "Lambda to run the Locust regression suite against the ${local.env} BFD Server"
  tags          = local.common_tags

  image_uri    = local.docker_image_uri
  package_type = "Image"

  memory_size = 2048
  timeout     = 600 # NOTE: 600 seconds or 10 minutes
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
  visibility_timeout_seconds = 600
}
