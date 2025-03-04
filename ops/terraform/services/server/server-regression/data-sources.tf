data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_ecr_authorization_token" "token" {}

data "aws_kms_key" "cmk" {
  key_id = local.kms_master_key_alias
}

data "aws_kms_key" "mgmt_key" {
  key_id = "alias/bfd-mgmt-cmk"
}

data "aws_kms_key" "config_cmk" {
  key_id = local.kms_config_key_alias
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

data "aws_kms_key" "insights_s3" {
  key_id = "alias/bfd-insights-bfd-cmk"
}

data "aws_iam_role" "insights" {
  name = "bfd-insights-bfd-glue-role"
}

data "aws_s3_bucket" "insights" {
  bucket = "bfd-insights-bfd-${data.aws_caller_identity.current.account_id}"
}

data "archive_file" "glue_trigger" {
  type        = "zip"
  source_file = "${path.module}/lambda-src/glue-trigger/glue-trigger.py"
  output_path = "${path.module}/lambda-src/glue-trigger/out/glue-trigger.zip"
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "archive_file" "spice_trigger" {
  type        = "zip"
  source_file = "${path.module}/lambda-src/spice-trigger/spice-trigger.py"
  output_path = "${path.module}/lambda-src/spice-trigger/out/spice-trigger.zip"
}
