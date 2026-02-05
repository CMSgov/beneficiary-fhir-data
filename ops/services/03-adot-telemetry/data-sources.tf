
data "aws_ecs_cluster" "main" {
  cluster_name = "bfd-${local.env}-cluster"
}
