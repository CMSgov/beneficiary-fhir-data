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
  service    = "server-load"

  vpc_name   = "bfd-${local.env}-vpc"
  queue_name = "bfd-${local.env}-${local.service}"

  docker_image_tag_node = coalesce(var.docker_image_tag_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag_node.value))
  docker_image_uri_node = "${data.aws_ecr_repository.ecr.repository_url}:${local.docker_image_tag_node}"

  docker_image_tag_controller = coalesce(var.docker_image_tag_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag_controller.value))
  docker_image_uri_controller = "${data.aws_ecr_repository.ecr.repository_url}:${local.docker_image_tag_controller}"

  docker_image_tag_broker = coalesce(var.docker_image_tag_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag_broker.value))
  docker_image_uri_broker = "${data.aws_ecr_repository.ecr.repository_url}:${local.docker_image_tag_broker}"

  lambda_timeout_seconds = 600
  kms_key_arn            = data.aws_kms_key.cmk.arn
  kms_key_id             = data.aws_kms_key.cmk.key_id
}

resource "aws_lambda_function" "controller" {
  function_name = "bfd-${local.env}-${local.service}-controller"
  description   = "Lambda to run the Locust controller for load testing on the ${local.env} server"
  tags          = local.shared_tags
  kms_key_arn   = local.kms_key_arn

  image_uri    = local.docker_image_uri_controller
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

resource "aws_lambda_function" "node" {
  function_name = "bfd-${local.env}-${local.service}-node"
  description   = "Lambda to run the Locust worker node for load testing on the ${local.env} server"
  tags          = local.shared_tags
  kms_key_arn   = local.kms_key_arn

  image_uri    = local.docker_image_uri_node
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

resource "aws_lambda_function" "broker" {
  function_name = "bfd-${local.env}-${local.service}-broker"
  description   = "Lambda to run the broker for load testing on the ${local.env} server"
  tags          = local.shared_tags
  kms_key_arn   = local.kms_key_arn

  image_uri    = local.docker_image_uri_broker
  package_type = "Image"

  memory_size = 2048
  timeout     = local.lambda_timeout_seconds
  environment {
    variables = {
      BFD_ENVIRONMENT = local.env
      SQS_QUEUE_NAME = resource.aws_sqs_queue.broker.name
      CONTROLLER_LAMBDA_NAME = resource.aws_lambda_function.controller.name
      NODE_LAMBDA_NAME = resource.aws_lambda_function.node.name
    }
  }

  role = aws_iam_role.this.arn
  vpc_config {
    security_group_ids = [aws_security_group.this.id]
    subnet_ids         = data.aws_subnets.main.ids
  }
}

resource "aws_lambda_event_source_mapping" "broker_run" {
  event_source_arn = aws_sqs_queue.broker_run_queue.arn
  function_name    = aws_lambda_function.broker.arn
}

resource "aws_sqs_queue" "broker_run_queue" {
  name                       = "${local.queue_name}-broker-run"
  visibility_timeout_seconds = local.lambda_timeout_seconds
  kms_master_key_id          = local.kms_key_id
}

resource "aws_sqs_queue" "broker" {
  name                       = "${local.queue_name}-broker"
  visibility_timeout_seconds = local.lambda_timeout_seconds
  kms_master_key_id          = local.kms_key_id
}

resource "aws_sns_topic" "scaling_topic" {
  name = "${local.queue_name}-scaling"
}

# Send scaling notifications from SNS to the SQS queue.
resource "aws_sns_topic_subscription" "scaling_subscription" {
  topic_arn = aws_sns_topic.scaling_topic.arn
  protocol = "sqs"
  endpoint = aws_sqs_queue.broker.arn
}

resource "aws_autoscaling_notification" "asn" {
  group_names = [
    data.aws_auto_scaling_group.asg.name
  ]

  notifications = [
    "autoscaling:EC2_INSTANCE_LAUNCH",
  ]

  topic_arn = aws_sns_topic.scaling_topic
}
