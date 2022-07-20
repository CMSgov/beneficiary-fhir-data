data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_ecr_authorization_token" "token" {}

data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${local.env}-cmk"
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

data "aws_ssm_parameter" "docker_image_uri" {
  name = "/bfd/mgmt/server/nonsensitive/locust_regression_suite_latest_image_uri"
}