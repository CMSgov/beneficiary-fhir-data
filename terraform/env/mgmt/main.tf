provider "aws" {
  region = "us-east-1"
}

data "aws_vpc" "shared_services" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

data "aws_subnet_ids" "app_subnets" {
  vpc_id = data.aws_vpc.shared_services.id

  tags = {
    Layer = "app"
  }
}

data "aws_subnet_ids" "dmz_subnets" {
  vpc_id = data.aws_vpc.shared_services.id

  tags = {
    Layer = "dmz"
  }
}

data "aws_subnet" "selected" {
  count = length(data.aws_subnet_ids.app_subnets.ids)
  id    = tolist(data.aws_subnet_ids.app_subnets.ids)[count.index]
}

/* ------ Jenkins ------- */

resource "aws_ebs_volume" "jenkins_data" {
  availability_zone = data.aws_subnet.selected[0].availability_zone
  size              = 500
  type              = "gp2"

  tags = {
    Name       = "bfd-jenkins-data"
    cpm_backup = "4HR Daily Weekly Monthly"
  }
}

module "jenkins" {
  source = "../../modules/resources/jenkins"

  vpc_id                = data.aws_vpc.shared_services.id
  app_subnets           = [data.aws_subnet_ids.app_subnets.ids]
  elb_subnets           = [data.aws_subnet_ids.dmz_subnets.ids]
  vpn_security_group_id = var.vpn_security_group_id
  ami_id                = var.jenkins_ami_id
  key_name              = var.jenkins_key_name
  tls_cert_arn          = var.jenkins_tls_cert_arn
}

/* ------ Packer IAM ------- */

resource "aws_iam_role" "packer" {
  name = "bfd-packer"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF

}

resource "aws_iam_instance_profile" "packer" {
  name = "bfd-packer"
  role = aws_iam_role.packer.name
}

resource "aws_iam_policy" "packer_s3" {
  name = "bfd-packer-s3"
  description = "packer S3 Policy"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "BFDProfile",
            "Effect": "Allow",
            "Action": [
                "s3:GetObjectAcl",
                "s3:GetObject",
                "s3:GetObjectVersionAcl",
                "s3:GetObjectTagging",
                "s3:ListBucket",
                "s3:GetObjectVersion"
            ],
            "Resource": [
                "arn:aws:s3:::bfd-packages/*",
                "arn:aws:s3:::bfd-packages"
            ]
        }
    ]
}
EOF

}

resource "aws_iam_role_policy_attachment" "packer_S3" {
role       = aws_iam_role.packer.name
policy_arn = aws_iam_policy.packer_s3.arn
}
