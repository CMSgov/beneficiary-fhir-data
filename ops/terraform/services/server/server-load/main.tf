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

  docker_image_tag_broker = split(":", coalesce(var.docker_image_tag_broker_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag_broker.value)))[1]
  docker_image_uri_broker = "${data.aws_ecr_repository.ecr_broker.repository_url}:${local.docker_image_tag_broker}"

  docker_image_tag_controller = split(":", coalesce(var.docker_image_tag_controller_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag_controller.value)))[1]
  docker_image_uri_controller = "${data.aws_ecr_repository.ecr_controller.repository_url}:${local.docker_image_tag_controller}"

  docker_image_tag_node = split(":", coalesce(var.docker_image_tag_node_override, nonsensitive(data.aws_ssm_parameter.docker_image_tag_node.value)))[1]
  docker_image_uri_node = "${data.aws_ecr_repository.ecr_node.repository_url}:${local.docker_image_tag_node}"

  lambda_timeout_seconds = 360
  kms_key_arn            = data.aws_kms_key.cmk.arn
  kms_key_id             = data.aws_kms_key.cmk.key_id

  ami_id                     = "ami-032b55183ff04af12" #TODO: whatever
  instance_type              = "m5.large"              #TODO: is this cool? We think so.
  volume_size                = "40"                    # TODO: this even might be too big. Who knows!
  nonsensitive_common_map    = zipmap(data.aws_ssm_parameters_by_path.nonsensitive_common.names, nonsensitive(data.aws_ssm_parameters_by_path.nonsensitive_common.values))
  nonsensitive_common_config = { for key, value in local.nonsensitive_common_map : split("/", key)[5] => value }
  key_pair                   = local.nonsensitive_common_config["key_pair"]
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
      SQS_QUEUE_NAME  = aws_sqs_queue.broker.name
    }
  }

  role = aws_iam_role.lambda.arn
  vpc_config {
    security_group_ids = [aws_security_group.lambda.id]
    subnet_ids         = data.aws_subnets.main.ids
  }
}

resource "aws_instance" "this" {
  count         = var.create_locust_instance ? 1 : 0
  ami           = local.ami_id
  instance_type = local.instance_type
  key_name      = local.key_pair

  iam_instance_profile        = aws_iam_instance_profile.this.name
  availability_zone           = "us-east-1b" # TODO
  tags                        = local.shared_tags
  monitoring                  = false
  associate_public_ip_address = false
  ebs_optimized               = true

  subnet_id              = data.aws_subnet.main.id
  vpc_security_group_ids = [data.aws_security_group.vpn.id, aws_security_group.lambda.id]

  root_block_device {
    tags                  = merge(local.shared_tags, { snapshot = "false" })
    volume_type           = "gp2"
    volume_size           = local.volume_size
    delete_on_termination = true
    encrypted             = true
    kms_key_id            = local.kms_key_arn
  }

  user_data = templatefile("${path.module}/locust-user-data.tftpl", {
    env              = local.env
    git_repo_version = var.git_repo_version # TODO: This works for now, but it's probably more appropriate for image to contain ansible configuration
  })
}

resource "aws_sqs_queue" "broker" {
  name                       = "${local.queue_name}-broker"
  visibility_timeout_seconds = 0
  kms_master_key_id          = local.kms_key_id
}

resource "aws_sqs_queue_policy" "broker" {
  queue_url = aws_sqs_queue.broker.id

  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Id": "${local.queue_name}-broker-sns-to-sqs",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "sns.amazonaws.com"
      },
      "Action": "SQS:SendMessage",
      "Resource": "${aws_sqs_queue.broker.arn}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${aws_sns_topic.sns.arn}"
        }
      }
    }
  ]
}
EOF
}

resource "aws_autoscaling_notification" "autoscaling_notification" {
  group_names = [
    data.aws_autoscaling_group.asg.name,
  ]

  notifications = [
    "autoscaling:EC2_INSTANCE_LAUNCH",
  ]

  topic_arn = aws_sns_topic.sns.arn
}

resource "aws_sns_topic" "sns" {
  name              = "${local.queue_name}-sns"
  # kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_subscription" "sqs_subscription" {
  topic_arn = aws_sns_topic.sns.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.broker.arn
}
