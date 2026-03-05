terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6"
    }
  }
}

module "terraservice" {
  source = "../../terraform-modules/bfd/bfd-terraservice"

  relative_module_root = "ops/services/03-repack"
  service              = local.service
  subnet_layers        = ["private"]

  additional_tags = {
    Name = local.name_prefix
    role = local.service
  }
}

locals {
  service = "repack"

  region                   = module.terraservice.region
  account_id               = module.terraservice.account_id
  default_tags             = module.terraservice.default_tags
  env                      = module.terraservice.env
  is_ephemeral_env         = module.terraservice.is_ephemeral_env
  bfd_version              = module.terraservice.bfd_version
  ssm_config               = module.terraservice.ssm_config
  env_key_arn              = module.terraservice.env_key_arn
  iam_path                 = module.terraservice.default_iam_path
  permissions_boundary_arn = module.terraservice.default_permissions_boundary_arn
  vpc                      = module.terraservice.vpc
  data_subnets             = module.terraservice.subnets_map["private"]

  name_prefix = "bfd-${local.env}-${local.service}"

  db_cluster_identifier = "bfd-${local.env}-aurora-cluster"
  rds_writer_az         = module.data_db_writer_instance.writer.availability_zone
  # ECS Fargate does not allow specifying the AZ, but it does allow for specifying the subnet. So,
  # we can control which AZ the pipeline service/task is placed into by filtering the list of
  # subnets by AZ
  writer_adjacent_subnets = [for subnet in local.data_subnets : subnet.id if subnet.availability_zone == local.rds_writer_az]
}

module "data_db_writer_instance" {
  source = "../../terraform-modules/general/data-db-writer-instance"

  cluster_identifier = data.aws_rds_cluster.main.cluster_identifier
}

data "aws_rds_cluster" "main" {
  cluster_identifier = local.db_cluster_identifier
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

data "aws_ami" "amazon_linux_2023_arm64" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*6.12*"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

data "aws_ec2_managed_prefix_list" "vpn" {
  filter {
    name   = "prefix-list-name"
    values = ["cmscloud-vpn"]
  }
}

resource "aws_cloudwatch_log_group" "this" {
  name         = "/aws/ecs/${local.name_prefix}"
  kms_key_id   = local.env_key_arn
  skip_destroy = true
}

resource "tls_private_key" "this" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "this" {
  key_name_prefix = local.name_prefix
  public_key      = tls_private_key.this.public_key_openssh
}

resource "aws_security_group" "this" {
  lifecycle {
    create_before_destroy = true
  }
  revoke_rules_on_delete = true

  name_prefix = "${local.name_prefix}-sg"
  description = "Allow ${local.service} egress to database; ingress from SSH within VPC"
  vpc_id      = local.vpc.id
  tags        = { Name = "${local.name_prefix}-sg" }
}

# Necessary since we do not use PrivateLink or service endpoints for any AWS Services, so this
# Instance must be able to connect to any address
resource "aws_vpc_security_group_egress_rule" "allow_all_traffic_ipv4" {
  security_group_id = aws_security_group.this.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1" # semantically equivalent to all ports
}

resource "aws_vpc_security_group_ingress_rule" "allow_ssh" {
  security_group_id = aws_security_group.this.id
  prefix_list_id    = data.aws_ec2_managed_prefix_list.vpn.id
  from_port         = 22
  ip_protocol       = "tcp"
  to_port           = 22
  description       = "Grants CMS VPN users access to SSH into the ${local.name_prefix} EC2 Instance"
}

resource "aws_vpc_security_group_ingress_rule" "allow_repack" {
  security_group_id            = data.aws_security_group.aurora_cluster.id
  referenced_security_group_id = aws_security_group.this.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "TCP"
  description                  = "Grants the ${local.name_prefix} EC2 Instance access to the ${local.env} database"
}

resource "aws_instance" "this" {
  ami                  = data.aws_ami.amazon_linux_2023_arm64.id
  key_name             = aws_key_pair.this.key_name
  instance_type        = "t4g.micro"
  iam_instance_profile = aws_iam_instance_profile.this.name

  disable_api_stop        = true
  disable_api_termination = true
  force_destroy           = true

  subnet_id              = local.writer_adjacent_subnets[0]
  vpc_security_group_ids = [aws_security_group.this.id]
}
