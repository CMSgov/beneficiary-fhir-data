data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

data "aws_rds_cluster" "main" {
  cluster_identifier = local.db_cluster_identifier
}

module "data_db_writer_instance" {
  source = "../../terraform-modules/general/data-db-writer-instance"

  cluster_identifier = data.aws_rds_cluster.main.cluster_identifier
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
