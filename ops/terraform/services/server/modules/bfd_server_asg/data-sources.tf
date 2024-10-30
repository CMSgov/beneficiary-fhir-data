# subnets
data "aws_subnet" "app_subnets" {
  count             = length(var.env_config.azs)
  vpc_id            = var.env_config.vpc_id
  availability_zone = var.env_config.azs[count.index]
  filter {
    name   = "tag:Layer"
    values = [var.layer]
  }
}

# kms master key
data "aws_kms_key" "master_key" {
  key_id = var.kms_key_alias
}

data "aws_rds_cluster" "rds" {
  cluster_identifier = var.db_config.db_cluster_identifier
}

data "external" "rds" {
  program = [
    "${path.module}/scripts/rds-cluster-config.sh", # helper script
    data.aws_rds_cluster.rds.cluster_identifier,    # verified, positional argument to script
    local.env                                       # environment name, almost exclusively here to provide beta reader functionality for production
  ]
}

data "external" "current_asg" {
  program = [
    "${path.module}/scripts/asg-data.sh", # helper script
    local.env
  ]
}

data "aws_launch_template" "main" {
  name = "bfd-${local.env}-${var.role}"
}
