locals {
  default_tags = {
    Environment    = local.env
    Layer          = local.layer
    Name           = "bfd-${local.env}-${local.service}"
    application    = "bfd"
    business       = "oeda"
    role           = local.service
    stack          = local.env
    Terraform      = true
    tf_module_root = "ops/terraform/services/server/server-load"
  }

  account_id             = data.aws_caller_identity.current.account_id
  availability_zone_name = var.create_locust_instance ? data.aws_availability_zones.this.names[random_integer.this[0].result] : ""
  env                    = terraform.workspace
  layer                  = "app"
  service                = "server-load"

  queue_name = "bfd-${local.env}-${local.service}"

  container_image_tag_node = split(":", coalesce(var.container_image_tag_node_override, nonsensitive(data.aws_ssm_parameter.container_image_tag_node.value)))[1]
  container_image_uri_node = "${data.aws_ecr_repository.ecr_node.repository_url}:${local.container_image_tag_node}"

  # We set the lambda timeout to the smallest value between the maximum timeout of 15 minutes (900
  # seconds) or the user-provided runtime limit plus an additional 15 seconds to allow the node
  # to cleanup properly
  lambda_timeout_seconds = min(900, var.test_runtime_limit + 15)

  kms_key_arn = data.aws_kms_key.cmk.arn
  kms_key_id  = data.aws_kms_key.cmk.key_id

  ami_id                     = data.aws_ami.main.id
  instance_type              = "m5.large"
  volume_size                = "40"
  nonsensitive_common_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_common.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values))
  nonsensitive_common_config = { for key, value in local.nonsensitive_common_map : split("/", key)[5] => value }
  key_pair                   = local.nonsensitive_common_config["key_pair"]
}

resource "aws_lambda_function" "node" {
  function_name = "bfd-${local.env}-${local.service}-node"
  description   = "Lambda to run the Locust worker node for load testing on the ${local.env} server"
  kms_key_arn   = local.kms_key_arn
  image_uri     = local.container_image_uri_node
  package_type  = "Image"
  memory_size   = 2048
  timeout       = local.lambda_timeout_seconds
  role          = aws_iam_role.lambda.arn

  environment {
    variables = {
      BFD_ENVIRONMENT      = local.env
      SQS_QUEUE_NAME       = aws_sqs_queue.this.name
      AWS_CURRENT_REGION   = data.aws_region.current.name
      ASG_NAME             = data.aws_autoscaling_group.asg.name
      COASTING_TIME        = var.coasting_time
      WARM_INSTANCE_TARGET = var.warm_instance_target
      STOP_ON_SCALING      = var.stop_on_scaling
    }
  }

  vpc_config {
    security_group_ids = [aws_security_group.this.id]
    subnet_ids         = data.aws_subnets.main.ids
  }
}

resource "aws_instance" "this" {
  count = var.create_locust_instance ? 1 : 0

  ami                         = local.ami_id
  associate_public_ip_address = false
  availability_zone           = local.availability_zone_name
  ebs_optimized               = true
  iam_instance_profile        = aws_iam_instance_profile.this.name
  instance_type               = local.instance_type
  key_name                    = local.key_pair
  monitoring                  = false

  subnet_id              = data.aws_subnet.main[0].id
  vpc_security_group_ids = [data.aws_security_group.vpn.id, aws_security_group.this.id]

  root_block_device {
    tags                  = local.default_tags
    volume_type           = "gp2"
    volume_size           = local.volume_size
    delete_on_termination = true
    encrypted             = true
    kms_key_id            = local.kms_key_arn
  }

  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    account_id         = local.account_id
    env                = local.env
    aws_current_region = data.aws_region.current.name
    asg_name           = data.aws_autoscaling_group.asg.name

    sqs_queue_name       = var.sqs_queue_name
    node_lambda_name     = var.node_lambda_name
    test_host            = var.test_host
    initial_worker_nodes = var.initial_worker_nodes
    node_spawn_time      = var.node_spawn_time
    max_spawned_nodes    = var.max_spawned_nodes
    max_spawned_users    = var.max_spawned_users
    user_spawn_rate      = var.user_spawn_rate
    test_runtime_limit   = var.test_runtime_limit
    coasting_time        = var.coasting_time
    warm_instance_target = var.warm_instance_target
    stop_on_scaling      = var.stop_on_scaling
    stop_on_node_limit   = var.stop_on_node_limit
  })
}

resource "random_integer" "this" {
  count = var.create_locust_instance ? 1 : 0
  min   = 0
  max   = 2
  keepers = {
    az = var.create_locust_instance
  }
}
