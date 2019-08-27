locals {
  tags        = merge({Layer=var.layer, role=var.role}, var.env_config.tags)
  is_prod     = substr(var.env_config.env, 0, 4) == "prod" 
}

# IAM: Setup Role, Profile and Policies for Jenkins

resource "aws_iam_role" "jenkins" {
  name = "bfd-mgmt-jenkins"

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
  name = "bfd-jenkins"
  role = "${aws_iam_role.jenkins.name}"
}

resource "aws_iam_policy" "jenkins_volume" {
  name        = "bfd-jenkins-volume"
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
  name        = "bfd-jenkins-permission-boundary"
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

### Add Jenkins user to group and attach policy to group

data "aws_iam_group" "managed_service" {
  group_name = "managed-service"
}

data "aws_iam_user" "jenkins" {
  user_name = "VZG9"
}

resource "aws_iam_group_membership" "managed_service_group" {
  name = "managed-service-group-membership"

  users = [
    "${data.aws_iam_user.jenkins.user_name}",
  ]

  group = "${data.aws_iam_group.managed_service.group_name}"
}

resource "aws_iam_policy_attachment" "jenkins_policy_boundary" {
  name       = "bfd-jenkins-permission-boundary"
  policy_arn = "${aws_iam_policy.jenkins_boundary.arn}"

  groups = ["${data.aws_iam_group.managed_service.group_name}"]
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
  name        = "packer_sg"
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
    name    = "tag:Layer"
    values  = [var.layer] 
  }
}

# Base security includes management SSH access
#

resource "aws_security_group" "base" {
  name          = "bfd-${var.env_config.env}-${var.role}-base"
  description   = "Allow CI access to app servers"
  vpc_id        = var.env_config.vpc_id
  tags          = merge({Name="bfd-${var.env_config.env}-${var.role}-base"}, local.tags)

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
  count         = var.lb_config == null ? 0 : 1
  name          = "bfd-${var.env_config.env}-${var.role}-app"
  description   = "Allow access to app servers"
  vpc_id        = var.env_config.vpc_id
  tags          = merge({Name="bfd-${var.env_config.env}-${var.role}-app"}, local.tags)

  ingress {
    from_port       = var.lb_config.port
    to_port         = var.lb_config.port
    protocol        = "tcp"
    # TODO: Figure out what the real ingress rule should be
    cidr_blocks     = ["10.0.0.0/8"]
  } 
}

##
# Launch configuration
##

resource "aws_launch_configuration" "main" {
  # Generate a new config on every revision
  name_prefix                 = "bfd-${var.env_config.env}-${var.role}-"
  security_groups             = concat([aws_security_group.base.id], aws_security_group.app[*].id)
  key_name                    = var.launch_config.key_name
  image_id                    = var.launch_config.ami_id
  instance_type               = var.launch_config.instance_type
  associate_public_ip_address = false
  iam_instance_profile        = var.launch_config.profile
  placement_tenancy           = local.is_prod ? "dedicated" : "default"

  user_data                   = templatefile("${path.module}/../templates/user_data.tpl", {
    env    = var.env_config.env
  })

  lifecycle {
    create_before_destroy = true
  }
}

##
# Autoscaling group
##
resource "aws_autoscaling_group" "main" {
  # Generate a new config on every revision
  name_prefix               = "bfd-${aws_launch_configuration.main.name}"
  desired_capacity          = var.asg_config.desired
  max_size                  = var.asg_config.max
  min_size                  = var.asg_config.min

  min_elb_capacity          = var.asg_config.desired
  wait_for_elb_capacity     = var.asg_config.desired
  wait_for_capacity_timeout = "10m"

  health_check_grace_period = 300
  health_check_type         = var.lb_config == null ? "EC2" : "ELB" # Failures of ELB healthchecks are asg failures
  vpc_zone_identifier       = data.aws_subnet.app_subnets[*].id
  launch_configuration      = aws_launch_configuration.main.name
  target_group_arns         = var.lb_config == null ? [] : [var.lb_config.tg_arn]

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
    key                   = "Name"
    value                 = "bfd-${var.env_config.env}-${var.role}"
    propagate_at_launch   = true
  }
}
