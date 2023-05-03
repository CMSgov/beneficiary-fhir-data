locals {
  account_id = data.aws_caller_identity.current.account_id
  env        = "mgmt"
  service    = "eft"
  layer      = "data"
  azs        = ["us-east-1a", "us-east-1b", "us-east-1c"]
  full_name  = "bfd-${local.env}-${local.service}"

  vpc_id = data.aws_vpc.this.arn

  additional_tags = {
    Layer = local.layer
    role  = local.service
  }

  sftp_port = 22
}

resource "aws_lb" "this" {
  name               = local.full_name
  load_balancer_type = "network"
  tags               = local.additional_tags

  subnets = data.aws_subnet.this[*].id

  access_logs {
    enabled = true
    bucket  = data.aws_s3_bucket.logs.id
    prefix  = local.full_name
  }
}

resource "aws_security_group" "this" {
  name        = "${local.full_name}-nlb"
  description = "Allow access to the ${local.service} network load balancer"
  vpc_id      = local.vpc_id
  tags        = merge({ Name = "${local.full_name}-nlb" }, local.additional_tags)

  ingress {
    from_port   = local.sftp_port
    to_port     = local.sftp_port
    protocol    = "tcp"
    cidr_blocks = [] # TODO: Determine CIDR
    description = "Allow ingress from SFTP traffic"
  }

  egress {
    from_port   = local.sftp_port # TODO: Is this correct?
    to_port     = local.sftp_port # TODO: Is this correct?
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.this.cidr_block]
  }
}

resource "aws_s3_bucket_policy" "logs" {
  bucket = data.aws_s3_bucket.logs.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "EFTNLBAccessLogs",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
          "AWS": "${data.aws_elb_service_account.this.arn}"
      },
      "Action": "s3:PutObject",
      "Resource": "${data.aws_s3_bucket.logs.arn}/${local.full_name}*"
    }
  ]
}
POLICY
}

