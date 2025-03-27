data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = [var.vpc_name]
  }
}

data "aws_kms_key" "cmk" {
  key_id = var.kms_key_alias
}

data "aws_kms_key" "config_cmk" {
  key_id = var.kms_config_key_alias
}

data "aws_ecr_repository" "ecr" {
  name = "bfd-mgmt-${local.service}-${local.lambda_name}-lambda"
}

data "aws_ecr_image" "this" {
  repository_name = data.aws_ecr_repository.ecr.name
  image_tag       = var.bfd_version
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

data "aws_security_group" "rds" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [var.db_cluster_identifier]
  }
}

data "aws_rds_cluster" "cluster" {
  cluster_identifier = var.db_cluster_identifier
}

data "aws_ssm_parameter" "alert_topics" {
  name = "/bfd/${local.env}/${local.service}/nonsensitive/ccw/slo/weekend_data_availability/verifier/alert_topics"
}

data "aws_sns_topic" "alert_topic" {
  count = length(local.alert_topics)
  name  = local.alert_topics[count.index]
}

data "aws_s3_bucket" "etl_bucket" {
  bucket = var.etl_bucket_id
}

data "aws_iam_policy" "permissions_boundary" {
  name = "ct-ado-poweruser-permissions-boundary-policy"
}
