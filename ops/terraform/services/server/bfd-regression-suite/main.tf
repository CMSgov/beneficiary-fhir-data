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

  account_id          = data.aws_caller_identity.current.account_id
  env                 = terraform.workspace
  layer               = "app"
  service             = "locust-regression"

  vpc_name            = "bfd-${local.env}-vpc"

  docker_image_uri                 = coalesce(var.docker_image_uri_override, data.aws_ssm_parameter.docker_image_uri)
  regression_suite_api_version     = var.regression_suite_api_version
  regression_suite_spawn_rate      = var.regression_suite_spawn_rate
  regression_suite_num_users       = var.regression_suite_num_users
  regression_suite_spawned_runtime = var.regression_suite_spawned_runtime
}

resource "aws_ecr_repository" "this" {
  name = "bfd-mgmt-${local.service}"
}

resource "aws_lambda_function" "this" {
  function_name = "bfd-${local.env}-${local.service}"
  description   = "Lambda to run the Locust regression suite against the ${local.env} BFD Server"
  tags          = local.common_tags

  image_uri    = local.docker_image_uri
  package_type = "Image"

  memory_size = 2048
  timeout     = 600
  environment {
    variables = {
      LOCUST_HOST                  = "https://${local.env}.bfd.cms.gov"
      LOCUST_LOCUSTFILE            = "/var/task/${local.regression_suite_api_version}/regression_suite.py"
      LOCUST_SPAWN_RATE            = local.regression_suite_spawn_rate
      LOCUST_USERS                 = local.regression_suite_num_users
      LOCUST_USERS_SPAWNED_RUNTIME = local.regression_suite_spawned_runtime
    }
  }

  role = aws_iam_role.this.arn
  vpc_config {
    security_group_ids = [aws_security_group.this.id]
    subnet_ids = data.aws_subnets.main.ids
  }
}