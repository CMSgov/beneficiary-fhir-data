# EFS
# 
# Create a EFS Resource, Mount Target and Security Group  
#

locals {
  tags        = merge({Layer=var.layer, role=var.role}, var.env_config.tags) 
}

data "aws_subnet" "main" {
  vpc_id            = var.env_config.vpc_id
  availability_zone = var.az
  filter {
    name    = "tag:Layer"
    values  = [var.layer] 
  }
}

resource "aws_efs_file_system" "efs" {
   creation_token = "bfd-${var.env_config.env}-${var.role}-efs"
   performance_mode = "generalPurpose"
   throughput_mode = "bursting"
   encrypted = "true"
 
  tags = {
    Name = "bfd-${var.env_config.env}-${var.role}-efs"
  }
}

resource "aws_efs_mount_target" "efs-mt" {
  file_system_id  = "${aws_efs_file_system.efs.id}"
  subnet_id = data.aws_subnet.main.id
  security_groups = ["${aws_security_group.ingress-efs.id}"]
}

 resource "aws_security_group" "ingress-efs" {
   name = "ingress-efs-test-sg"
   name = "bfd-${var.env_config.env}-${var.role}-efs"
   vpc_id = "${aws_vpc.test-env.id}"

   // NFS
   ingress {
     security_groups = ["${aws_security_group.ingress-test-env.id}"]
     from_port = 2049
     to_port = 2049
     protocol = "tcp"
   }

   // Terraform removes the default rule
   egress {
     security_groups = ["${aws_security_group.ingress-test-env.id}"]
     from_port = 0
     to_port = 0
     protocol = "-1"
   }
 }