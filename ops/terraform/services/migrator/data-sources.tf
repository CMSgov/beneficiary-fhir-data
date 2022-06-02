data "external" "rds" {
  program = [
    "${path.module}/scripts/rds-cluster-config.sh", # helper script
    data.aws_rds_cluster.rds.cluster_identifier     # verified, positional argument to script
  ]
}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "main" {
  key_id = "alias/bfd-${local.env}-cmk"
}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

data "aws_subnet" "main" {
  vpc_id            = data.aws_vpc.main.id
  availability_zone = data.external.rds.result["WriterAZ"]
  filter {
    name   = "tag:Layer"
    values = [local.layer]
  }
}

data "aws_security_group" "vpn" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpn-private"]
  }
}

data "aws_security_group" "rds" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-aurora-cluster"]
  }
}

# These data sources aren't very useful, but they do verify inputs to terraform
# ensuring that the resources exist before offered to the configuration
data "aws_key_pair" "main" {
  key_name = local.key_pair
}

data "aws_ami" "main" {
  owners = ["self"]
  filter {
    name   = "image-id"
    values = [local.ami_id]
  }
}

data "aws_rds_cluster" "rds" {
  cluster_identifier = local.rds_cluster_identifier
}
