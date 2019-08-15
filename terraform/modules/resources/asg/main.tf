locals {
  tags        = merge({layer=var.layer, role=var.role}, var.env_config.tags)
}

##
# Data providers
##

/* TODO: hookup to CI pipeline when its ready
data "aws_ami" "image" {
  most_recent = true
  owners      = ["self"]

  filter {
    name   = "image-id"
    values = [var.launch_config.ami_id]
  }
}
*/

# Subnets
# 
# Subnets are created by CCS VPC setup
#
data "aws_subnet" "app_subnets" {
  count     = length(var.env_config.azs)
  vpc_id    = var.env_config.vpc_id
  filter {
    name    = "tag:Name"
    values  = ["bfd-${var.env_config.env}-az${count.index+1}-${var.layer}" ] 
  }
}

##
# Security groups
##

# SSH access from management VPC
#
resource "aws_security_group" "ci" {
  name          = "bfd-${var.env_config.env}-${var.role}-ci"
  description   = "Allow CI access to app servers"
  vpc_id        = var.env_config.vpc_id
  tags          = merge({Name="bfd-${var.env_config.env}-${var.role}-ci"}, local.tags)

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.mgmt_config.ci_cidrs
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    security_groups = [var.mgmt_config.remote_sg,var.mgmt_config.vpn_sg,var.mgmt_config.tool_sg]
  }
}

# App access from the LB
#
resource "aws_security_group" "app" {
  name          = "bfd-${var.env_config.env}-${var.role}-app"
  description   = "Allow access to app servers"
  vpc_id        = var.env_config.vpc_id
  tags          = merge({Name="bfd-${var.env_config.env}-${var.role}-app"}, local.tags)

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

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
  security_groups             = [aws_security_group.ci.id, aws_security_group.app.id]
  key_name                    = var.launch_config.key_name
  image_id                    = var.launch_config.ami_id
  instance_type               = var.launch_config.instance_type
  associate_public_ip_address = false
  iam_instance_profile        = "bfd-${var.env_config.env}-${var.role}-profile"

  user_data                   = templatefile("${path.module}/templates/user_data.tpl", {
    env    = var.env_config.env
    bucket = var.launch_config.app_config_bucket
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
  name_prefix               = "bfd-${var.env_config.env}-${var.role}-"
  desired_capacity          = var.asg_config.desired
  max_size                  = var.asg_config.max
  min_size                  = var.asg_config.min

  # Make terraform wait for instances to join the ELB
  # per https://www.terraform.io/docs/providers/aws/r/autoscaling_group.html#waiting-for-capacity
  /* TODO: Wait for real AMI's before this step 
  min_elb_capacity          = var.asg_config.desired
  wait_for_elb_capacity     = var.asg_config.desired
  wait_for_capacity_timeout = "10m"
  */

  health_check_grace_period = 300
  health_check_type         = "ELB" # Failures of ELB healthchecks are asg failures
  vpc_zone_identifier       = data.aws_subnet.app_subnets[*].id
  launch_configuration      = aws_launch_configuration.main.name
  target_group_arns         = [var.lb_config.tg_arn]

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

  lifecycle {
    create_before_destroy = true
  }
}

##
# Autoscaling policies and Cloudwatch alarms
##
resource "aws_autoscaling_policy" "high-cpu" {
  name                   = "bfd-${var.env_config.env}-${var.role}-high-cpu-scaleup"
  scaling_adjustment     = 2
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.main.name
}

resource "aws_cloudwatch_metric_alarm" "high-cpu" {
  alarm_name          = "bfd-${var.env_config.env}-${var.role}-high-cpu"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 120
  statistic           = "Average"
  threshold           = 60

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.main.name
  }

  alarm_description = "CPU usage for ${aws_autoscaling_group.main.name} ASG"
  alarm_actions     = [aws_autoscaling_policy.high-cpu.arn]
}

resource "aws_autoscaling_policy" "low-cpu" {
  name                   = "bfd-${var.env_config.env}-${var.role}-low-cpu-scaledown"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 300
  autoscaling_group_name = aws_autoscaling_group.main.name
}

resource "aws_cloudwatch_metric_alarm" "low-cpu" {
  alarm_name          = "bfd-${var.env_config.env}-${var.role}-low-cpu"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 120
  statistic           = "Average"
  threshold           = 20

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.main.name
  }

  alarm_description = "CPU usage for ${aws_autoscaling_group.main.name} ASG"
  alarm_actions     = [aws_autoscaling_policy.low-cpu.arn]
}

##
# Autoscaling notifications
##
resource "aws_autoscaling_notification" "asg_notifications" {
  count = var.asg_config.sns_topic_arn != "" ? 1 : 0

  group_names = [aws_autoscaling_group.main.name]

  notifications = [
    "autoscaling:EC2_INSTANCE_LAUNCH",
    "autoscaling:EC2_INSTANCE_TERMINATE",
    "autoscaling:EC2_INSTANCE_LAUNCH_ERROR",
  ]
  
  topic_arn = var.asg_config.sns_topic_arn
}
