data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_ecr_authorization_token" "token" {}

data "aws_kms_key" "cmk" {
  key_id = "alias/bfd-${local.env}-cmk" # TODO: replace ssm lookup
}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_subnets" "main" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }
  filter {
    name   = "tag:Layer"
    values = [local.layer]
  }
}

data "aws_launch_template" "template" {
  name = "bfd-${local.env}-fhir"
}

data "aws_auto_scaling_group" "asg" {
  name = "${data.aws_launch_template.template.name}-${data.aws_launch_template.template.latest_version}"
}

data "aws_ecr_repository" "ecr" {
  name = "bfd-mgmt-${local.service}"
}

data "aws_ecr_image" "image_node" {
  repository_name = data.aws_ecr_repository.ecr.name
  image_tag       = local.docker_image_tag_node
}

data "aws_ecr_image" "image_controller" {
  repository_name = data.aws_ecr_repository.ecr.name
  image_tag       = local.docker_image_tag_controller
}

data "aws_ecr_image" "image_broker" {
  repository_name = data.aws_ecr_repository.ecr.name
  image_tag       = local.docker_image_tag_broker
}

data "aws_ssm_parameter" "docker_image_tag_node" {
  # TODO: consider making this more environment-specific, versioning RFC in BFD-1743 may inform us of how
  name = "/bfd/mgmt/server/nonsensitive/server_load_node_latest_image_tag"
}

data "aws_ssm_parameter" "docker_image_tag_controller" {
  # TODO: consider making this more environment-specific, versioning RFC in BFD-1743 may inform us of how
  name = "/bfd/mgmt/server/nonsensitive/server_load_controller_latest_image_tag"
}

data "aws_ssm_parameter" "docker_image_tag_broker" {
  # TODO: consider making this more environment-specific, versioning RFC in BFD-1743 may inform us of how
  name = "/bfd/mgmt/server/nonsensitive/server_load_broker_latest_image_tag"
}

data "aws_security_group" "rds" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-aurora-cluster"] # TODO think harder about this... RE: ssm, ephemeral environments, etc.
  }
}
