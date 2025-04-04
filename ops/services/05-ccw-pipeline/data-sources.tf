data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_rds_cluster" "main" {
  cluster_identifier = local.db_cluster_identifier
}

data "external" "writer_identifier" {
  program = [
    "${path.module}/scripts/rds-writer-identifier.sh", # helper script
    data.aws_rds_cluster.main.cluster_identifier       # verified, positional argument to script
  ]
}

data "aws_db_instance" "writer" {
  db_instance_identifier = data.external.writer_identifier.result.writer
}

data "aws_ecr_repository" "pipeline" {
  name = local.pipeline_repository_name
}

data "aws_ecr_image" "pipeline" {
  repository_name = data.aws_ecr_repository.pipeline.name
  image_tag       = local.pipeline_version
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
