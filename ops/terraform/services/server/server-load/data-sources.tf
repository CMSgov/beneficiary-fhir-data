data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

data "aws_kms_key" "cmk" {
  key_id = local.nonsensitive_common_config["kms_key_alias"]
}

# NOTE: locust load test controller needs a well-vetted bfd image
#       the db-migrator image was chosen somewhat arbitrarily
data "aws_ami" "main" {
  most_recent = true
  owners      = ["self"]
  name_regex  = ".*server-load.*"

  filter {
    name   = "tag:Branch"
    values = ["master"]
  }
}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = [local.nonsensitive_common_config["vpc_name"]]
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

# TODO: Consider hoisting ASG signaling logic into forthcoming server definition
data "aws_launch_template" "template" {
  name = "bfd-${local.env}-fhir"
}

data "aws_autoscaling_group" "asg" {
  name = "${data.aws_launch_template.template.name}-${data.aws_launch_template.template.latest_version}"
}

data "aws_ecr_repository" "ecr_node" {
  name = "bfd-mgmt-${local.service}-node"
}

data "aws_ecr_image" "image_node" {
  repository_name = data.aws_ecr_repository.ecr_node.name
  image_tag       = local.container_image_tag_node
}

# TODO: Consider making this more environment-specific, versioning RFC in BFD-1743 may provide us a path forward
data "aws_ssm_parameter" "container_image_tag_node" {
  name = "/bfd/mgmt/server/nonsensitive/server_load_node_latest_image_tag"
}

data "aws_security_group" "rds" {
  vpc_id = data.aws_vpc.main.id
  filter {
    # NOTE: The rds security group shares a name with the rds cluster identifer
    name   = "tag:Name"
    values = [local.nonsensitive_common_config["rds_cluster_identifier"]]
  }
}

data "aws_ssm_parameters_by_path" "nonsensitive_common" {
  path = "/bfd/${local.env}/common/nonsensitive"
}

data "aws_subnet" "main" {
  count             = var.create_locust_instance ? 1 : 0
  vpc_id            = data.aws_vpc.main.id
  availability_zone = local.availability_zone_name
  filter {
    name   = "tag:Layer"
    values = [local.layer]
  }
}

data "aws_security_group" "vpn" {
  vpc_id = data.aws_vpc.main.id
  filter {
    name   = "tag:Name"
    values = [local.nonsensitive_common_config["vpn_security_group"]]
  }
}

data "aws_availability_zones" "this" {
  state = "available"
}
