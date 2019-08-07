resource "aws_elb" "elb" {
  name     = "bfd-jenkins"
  internal = true

  subnets = flatten([
    var.elb_subnets,
  ])
  security_groups = [
    "${var.vpn_security_group_id}",
    "${aws_security_group.elb.id}",
  ]

  listener {
    instance_port     = 80
    instance_protocol = "http"
    lb_port           = 80
    lb_protocol       = "http"
  }

  listener {
    instance_port      = 80
    instance_protocol  = "http"
    lb_port            = 443
    lb_protocol        = "https"
    ssl_certificate_id = "${var.tls_cert_arn}"
  }

  health_check {
    healthy_threshold   = 2
    unhealthy_threshold = 5
    timeout             = 5
    target              = "HTTP:80/robots.txt"
    interval            = 10
  }

  cross_zone_load_balancing   = true
  idle_timeout                = 60
  connection_draining         = true
  connection_draining_timeout = 60

  tags = {
    Name = "bfd-jenkins"
  }
}

resource "aws_autoscaling_group" "asg" {
  name                      = "bfd-${aws_launch_configuration.lc.name}"
  launch_configuration      = "${aws_launch_configuration.lc.name}"
  max_size                  = 1
  min_size                  = 1
  health_check_grace_period = 600
  health_check_type         = "ELB"

  vpc_zone_identifier = flatten([
    var.app_subnets,
  ])

  load_balancers = flatten([
    aws_elb.elb.id,
  ])

  tag {
    key                 = "Name"
    value               = "bfd-jenkins"
    propagate_at_launch = true
  }

  tag {
    key                 = "role"
    value               = "jenkins"
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "elb" {
  name        = "elb"
  description = "ELB security group"
  vpc_id      = "${var.vpc_id}"
}

resource "aws_security_group" "allow_elb" {
  name        = "allow_elb"
  description = "Allow traffic from ELB"
  vpc_id      = "${var.vpc_id}"

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]

    security_groups = [
      "${aws_security_group.elb.id}",
    ]
  }

  ingress {
    from_port = 8080
    to_port   = 8080
    protocol  = "tcp"

    security_groups = [
      "${aws_security_group.elb.id}",
    ]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

resource "aws_launch_configuration" "lc" {
  image_id             = "${var.ami_id}"
  instance_type        = "${var.instance_type}"
  key_name             = "${var.key_name}"
  name_prefix          = "bfd-jenkins-"
  iam_instance_profile = "${aws_iam_instance_profile.jenkins.name}"
  security_groups = [
    "${aws_security_group.allow_elb.id}",
    "${var.vpn_security_group_id}",
  ]

  ebs_block_device { # Jenkins Data
    device_name                 = "/dev/sdb"
    volume_type                 = "gp2"
    volume_size                 = 10
    iops                        = 200
    encrypted                   = "true"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_iam_role" "jenkins_role" {
  name = "bfd-jenkins"

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
