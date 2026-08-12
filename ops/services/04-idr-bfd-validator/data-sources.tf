data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_rds_cluster" "main" {
  cluster_identifier = local.db_cluster_identifier
}

data "aws_ecr_repository" "this" {
  name = local.container_repository_name
}

data "aws_ecr_image" "this" {
  repository_name = data.aws_ecr_repository.this.name
  image_tag       = local.container_version
}

data "aws_security_group" "aurora_cluster" {
  filter {
    name   = "tag:Name"
    values = [data.aws_rds_cluster.main.cluster_identifier]
  }
  filter {
    name   = "vpc-id"
    values = [local.vpc.id]
  }
}

data "aws_sns_topic" "slack" {
  name = "bfd-platform-slack-bfd-internal-alerts"
}
