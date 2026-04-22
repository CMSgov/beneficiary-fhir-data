data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}

module "data_strategies" {
  source = "../../terraform-modules/bfd/bfd-data-ecs-strategies"

  service      = local.service
  ssm_config   = local.ssm_config
  cluster_name = data.aws_ecs_cluster.main.cluster_name
}

data "aws_rds_cluster" "main" {
  cluster_identifier = local.db_cluster_identifier
}

module "data_db_writer_instance" {
  source = "../../terraform-modules/general/data-db-writer-instance"

  cluster_identifier = data.aws_rds_cluster.main.cluster_identifier
}

data "aws_ecr_repository" "migrator" {
  name = local.migrator_repository_name
}

data "aws_ecr_image" "migrator" {
  repository_name = data.aws_ecr_repository.migrator.name
  image_tag       = local.migrator_version
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
