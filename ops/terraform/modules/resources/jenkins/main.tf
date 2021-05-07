locals {
  azs        = ["us-east-1a"]
  env_config = { env = var.env_config.env, tags = var.env_config.tags, azs = local.azs }
  tags       = merge({ Layer = var.layer, role = var.role }, var.env_config.tags)
  is_prod    = substr(var.env_config.env, 0, 4) == "prod"
}

# IAM: Setup Role, Profile and Policies for Jenkins

resource "aws_iam_role" "jenkins" {
  name = "bfd-${var.env_config.env}-jenkins"

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

resource "aws_iam_instance_profile" "jenkins_profile" {
  name = "bfd-${var.env_config.env}-jenkins"
  role = "${aws_iam_role.jenkins.name}"
}

resource "aws_iam_policy" "jenkins_volume" {
  name        = "bfd-${var.env_config.env}-jenkins-volume"
  description = "Jenkins Data Volume Policy"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "ec2:AttachVolume",
            "Resource": [
                "arn:aws:ec2:*:*:instance/*",
                "arn:aws:ec2:*:*:volume/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": "ec2:DescribeVolumes",
            "Resource": "*"
        }
    ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "jenkins_volume" {
  role       = "${aws_iam_role.jenkins.name}"
  policy_arn = "${aws_iam_policy.jenkins_volume.arn}"
}

resource "aws_iam_policy" "jenkins_boundary" {
  name        = "bfd-${var.env_config.env}-jenkins-permission-boundary"
  description = "Jenkins Permission Boundary Policy"
  path        = "/"

  policy = <<EOT
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "JenkinsPermissionBoundary",
      "Effect": "Allow",
      "Action": [
        "kms:List*",
        "rds:*",
        "ec2:Reset*",
        "logs:*",
        "cloudtrail:GetTrailStatus",
        "kms:Get*",
        "dynamodb:*",
        "autoscaling:*",
        "kms:ReEncrypt*",
        "iam:GetPolicy*",
        "rds:Describe*",
        "ec2:DeleteNetworkAcl*",
        "cloudtrail:ListTags",
        "iam:GetServiceLastAccessed*",
        "config:*",
        "events:*",
        "ec2:Associate*",
        "sns:*",
        "iam:GetRole",
        "cloudtrail:LookupEvents",
        "iam:GetGroup*",
        "kms:Describe*",
        "ec2:Cancel*",
        "cloudtrail:DescribeTrails",
        "iam:*",
        "cloudwatch:*",
        "ec2:Modify*",
        "ec2:*",
        "iam:GetAccount*",
        "ec2:AssignPrivateIpAddresses*",
        "iam:GetUser*",
        "ec2:Request*",
        "iam:ListAttached*",
        "cloudtrail:GetEventSelectors",
        "iam:PassRole",
        "ses:*",
        "kms:*",
        "ec2:Import*",
        "ec2:Release*",
        "iam:GetRole*",
        "ec2:Purchase*",
        "ec2:Bundle*",
        "elasticfilesystem:*",
        "s3:*",
        "ec2:Copy*",
        "ec2:Replace*",
        "iam:ListRoles",
        "sts:*",
        "elasticloadbalancing:*",
        "iam:Simulate*",
        "ec2:Describe*",
        "cloudtrail:ListPublicKeys",
        "route53:*",
        "iam:GetContextKeys*",
        "ec2:Allocate*",
        "iam:Upload*",
        "waf-regional:*"
      ],
      "Resource": "*"
    }
  ]
}
EOT
}

resource "aws_iam_role_policy_attachment" "jenkins_boundary" {
  role       = "${aws_iam_role.jenkins.name}"
  policy_arn = "${aws_iam_policy.jenkins_boundary.arn}"
}

### Lock Down Packer SG for Jenkins, Data App and Data Server

data "aws_security_group" "managed_public" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpn-public"]
  }
}

data "aws_security_group" "managed_private" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpn-private"]
  }
}

data "aws_vpc" "managed_vpc" {
  filter {
    name   = "tag:Name"
    values = ["bfd-mgmt-vpc"]
  }
}

resource "aws_security_group" "packer_sg" {
  name        = "bfd-${var.env_config.env}-packer_sg"
  description = "Allow traffic to Packer Instances"
  vpc_id      = "${var.vpc_id}"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["${data.aws_vpc.managed_vpc.cidr_block}"]

    security_groups = [
      "${data.aws_security_group.managed_public.id}",
      "${data.aws_security_group.managed_private.id}",
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "packer_sg"
  }
}

# Subnets are created by CCS VPC setup
#
data "aws_subnet" "app_subnets" {
  count             = length(var.env_config.azs)
  vpc_id            = var.env_config.vpc_id
  availability_zone = var.env_config.azs[0]
  filter {
    name   = "tag:Layer"
    values = [var.layer]
  }
}

# Base security includes management SSH access
#

resource "aws_security_group" "base" {
  name        = "bfd-${var.env_config.env}-${var.role}-base"
  description = "Allow CI access to app servers"
  vpc_id      = var.env_config.vpc_id
  tags        = merge({ Name = "bfd-${var.env_config.env}-${var.role}-base" }, local.tags)

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.mgmt_config.ci_cidrs
  }

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Callers access to the app
#
resource "aws_security_group" "app" {
  count       = var.lb_config == null ? 0 : 1
  name        = "bfd-${var.env_config.env}-${var.role}-app"
  description = "Allow access to app servers"
  vpc_id      = var.env_config.vpc_id
  tags        = merge({ Name = "bfd-${var.env_config.env}-${var.role}-app" }, local.tags)

  ingress {
    from_port = var.lb_config.port
    to_port   = var.lb_config.port
    protocol  = "tcp"
    # TODO: Figure out what the real ingress rule should be
    cidr_blocks = ["10.0.0.0/8"]
  }
}

##
# Launch template
##
resource "aws_launch_template" "main" {
  name                   = "bfd-${var.env_config.env}-${var.role}"
  description            = "Template for the ${var.env_config.env} environment ${var.role} servers"
  vpc_security_group_ids = concat([aws_security_group.base.id, var.mgmt_config.vpn_sg], aws_security_group.app[*].id)
  key_name               = var.launch_config.key_name
  image_id               = var.launch_config.ami_id
  instance_type          = var.launch_config.instance_type
  ebs_optimized          = true

  iam_instance_profile {
    name = "bfd-${var.env_config.env}-jenkins"
  }

  placement {
    tenancy = local.is_prod ? "dedicated" : "default"
  }

  monitoring {
    enabled = false
  }
}


##
# Autoscaling group
##
resource "aws_autoscaling_group" "main" {
  # Generate a new group on every revision of the launch template. 
  # This does a simple version of a blue/green deployment
  #
  name             = "${aws_launch_template.main.name}-${aws_launch_template.main.latest_version}"
  desired_capacity = var.asg_config.desired
  max_size         = var.asg_config.max
  min_size         = var.asg_config.min

  # If an lb is defined, wait for the ELB 
  min_elb_capacity          = var.lb_config == null ? null : var.asg_config.min
  wait_for_capacity_timeout = var.lb_config == null ? null : "20m"

  health_check_grace_period = 400
  health_check_type         = var.lb_config == null ? "EC2" : "ELB" # Failures of ELB healthchecks are asg failures
  vpc_zone_identifier       = data.aws_subnet.app_subnets[*].id
  load_balancers            = var.lb_config == null ? [] : [var.lb_config.name]

  launch_template {
    name    = aws_launch_template.main.name
    version = aws_launch_template.main.latest_version
  }

  enabled_metrics = [
    "GroupMinSize",
    "GroupMaxSize",
    "GroupDesiredCapacity",
    "GroupInServiceInstances",
    "GroupPendingInstances",
    "GroupStandbyInstances",
    "GroupTerminatingInstances",
    "GroupTotalInstances",
  ]

  dynamic "tag" {
    for_each = local.tags
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }

  tag {
    key                 = "Name"
    value               = "bfd-${var.env_config.env}-${var.role}"
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = false
  }
}

