# Create a EFS Resource, Mount Target and Security Group for our mgmt servers
#

locals {
  tags = merge({ Layer = var.layer, role = var.role }, var.env_config.tags)
}

data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_subnet" "main" {
  vpc_id            = data.aws_vpc.main.id
  availability_zone = var.env_config.azs
  filter {
    name   = "tag:Layer"
    values = [var.layer]
  }
}


##
# RESOURCES

resource "aws_efs_file_system" "efs" {
  creation_token   = "bfd-${var.env_config.env}-${var.role}-efs"
  performance_mode = "generalPurpose"
  throughput_mode  = "bursting"
  encrypted        = "true"

  tags = {
    Name = "bfd-${var.env_config.env}-${var.role}-efs"
  }
}

resource "aws_security_group" "efs-sg" {
  name        = "bfd-${var.env_config.env}-${var.role}-efs-sg"
  description = "EFS Security Group"
  vpc_id      = data.aws_vpc.main.id

  # NFS
  ingress {
    description = "Inbound access for EFS"
    from_port   = 2049
    to_port     = 2049
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.main.cidr_block]
  }

  #Terraform removes the default rule
  egress {
    description = "Outbound Access for EFS"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [data.aws_vpc.main.cidr_block]
  }
}

resource "aws_efs_mount_target" "efs-mnt" {
  file_system_id  = aws_efs_file_system.efs.id
  subnet_id       = data.aws_subnet.main.id
  security_groups = [aws_security_group.efs-sg.id]
}
