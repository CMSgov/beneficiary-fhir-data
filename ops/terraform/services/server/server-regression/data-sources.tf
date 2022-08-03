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

data "aws_ecr_repository" "ecr" {
  name = "bfd-mgmt-${local.service}"
}

data "aws_ecr_image" "image" {
  repository_name = data.aws_ecr_repository.ecr.name
  image_tag       = local.docker_image_tag
}

data "aws_ssm_parameter" "docker_image_tag" {
  # TODO: consider making this more environment-specific, versioning RFC in BFD-1743 may inform us of how
  name = "/bfd/mgmt/server/nonsensitive/server_regression_latest_image_tag"
}

data "aws_security_group" "rds" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-aurora-cluster"] # TODO think harder about this... RE: ssm, ephemeral environments, etc.
  }
}
