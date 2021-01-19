locals {
  tags        = merge({Layer=var.layer, role=var.role}, var.env_config.tags)
  is_prod     = substr(var.env_config.env, 0, 4) == "prod" 
}

##
# Data providers
##

# Subnets
# 
# Subnets are created by CCS VPC setup
#
data "aws_subnet" "app_subnets" {
  count             = length(var.env_config.azs)
  vpc_id            = var.env_config.vpc_id
  availability_zone = var.env_config.azs[count.index]
  filter {
    name    = "tag:Layer"
    values  = [var.layer] 
  }
}

# KMS 
#
# The customer master key is created outside of this script
#
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${var.env_config.env}-cmk"
}


##
# Create Resources
##

#
# Security groups
#

# Base security group with egress 
#
resource "aws_security_group" "base" {
  name          = "bfd-${var.env_config.env}-${var.role}-base"
  description   = "Allow CI access to app servers"
  vpc_id        = var.env_config.vpc_id
  tags          = merge({Name="bfd-${var.env_config.env}-${var.role}-base"}, local.tags)

  ingress       = []  # Make the ingress empty for this SG. 
  
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
    security_groups = [var.lb_config.sg]
  } 
}

# App access to the database
#
resource "aws_security_group_rule" "allow_db_access" {
  count                     = var.db_config == null ? 0 : 1
  type                      = "ingress"
  from_port                 = 5432
  to_port                   = 5432
  protocol                  = "tcp"
  description               = "Allows access to the ${var.db_config.role} db"

  security_group_id         = var.db_config.db_sg           # The SG associated with each replica
  source_security_group_id  = aws_security_group.app[0].id  # Every instance in the ASG
}

##
# Launch template
##
resource "aws_launch_template" "main" {
  name                          = "bfd-${var.env_config.env}-${var.role}"
  description                   = "Template for the ${var.env_config.env} environment ${var.role} servers"
  vpc_security_group_ids        = concat([aws_security_group.base.id, var.mgmt_config.vpn_sg], aws_security_group.app[*].id)
  key_name                      = var.launch_config.key_name
  image_id                      = var.launch_config.ami_id
  instance_type                 = var.launch_config.instance_type
  ebs_optimized                 = true

  iam_instance_profile {
    name                        = var.launch_config.profile
  }

  placement {
    tenancy                     = local.is_prod ? "dedicated" : "default"
  }

  monitoring {
    enabled = true
  }
  
  block_device_mappings {    
    device_name = "/dev/sda1"
    ebs {
      volume_type               = "gp2"
      volume_size               = var.launch_config.volume_size
      delete_on_termination     = true
      encrypted                 = true
      kms_key_id                = data.aws_kms_key.master_key.arn
    }
  }
  
  user_data = base64encode(templatefile("${path.module}/../templates/${var.launch_config.user_data_tpl}", {
    env           = var.env_config.env
    port          = var.lb_config.port
    accountId     = var.launch_config.account_id
    gitBranchName = var.launch_config.git_branch
    gitCommitId   = var.launch_config.git_commit
  }))

  tag_specifications {
    resource_type               = "instance"
    tags                        = merge({Name="bfd-${var.env_config.env}-${var.role}"}, local.tags)
  }

  tag_specifications {
    resource_type               = "volume"
    tags                        = merge({snapshot="true", Name="bfd-${var.env_config.env}-${var.role}"}, local.tags)
  }
}

##
# Autoscaling group
##
resource "aws_autoscaling_group" "main" {
  # Generate a new group on every revision of the launch template. 
  # This does a simple version of a blue/green deployment
  #
  name                      = "${aws_launch_template.main.name}-${aws_launch_template.main.latest_version}"
  desired_capacity          = var.asg_config.desired
  max_size                  = var.asg_config.max
  min_size                  = var.asg_config.min

  # If an lb is defined, wait for the ELB 
  min_elb_capacity          = var.lb_config == null ? null : var.asg_config.min
  wait_for_capacity_timeout = var.lb_config == null ? null : "20m"

  health_check_grace_period = var.asg_config.instance_warmup
  health_check_type         = var.lb_config == null ? "EC2" : "ELB" # Failures of ELB healthchecks are asg failures
  vpc_zone_identifier       = data.aws_subnet.app_subnets[*].id
  load_balancers            = var.lb_config == null ? [] : [var.lb_config.name]

  launch_template {
    name                    = aws_launch_template.main.name
    version                 = aws_launch_template.main.latest_version
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
  name                      = "bfd-${var.env_config.env}-${var.role}-high-cpu-policy"
  autoscaling_group_name    = aws_autoscaling_group.main.name
  adjustment_type           = "ChangeInCapacity"
  policy_type               = "StepScaling"
  estimated_instance_warmup = var.asg_config.instance_warmup
  metric_aggregation_type   = "Average"

  step_adjustment {
    scaling_adjustment          = 1
    metric_interval_lower_bound = 0.0
    metric_interval_upper_bound = 15.0
  }

  step_adjustment {
    scaling_adjustment          = 2
    metric_interval_lower_bound = 15.0
    metric_interval_upper_bound = 35.0
  }

  step_adjustment {
    scaling_adjustment          = 3
    metric_interval_lower_bound = 35.0
  }
}

resource "aws_cloudwatch_metric_alarm" "high-cpu" {
  alarm_name          = "bfd-${var.env_config.env}-${var.role}-high-cpu"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Average"
  threshold           = 35

  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.main.name
  }

  alarm_description = "CPU usage for ${aws_autoscaling_group.main.name} ASG"
  alarm_actions     = [aws_autoscaling_policy.high-cpu.arn]
}

resource "aws_autoscaling_policy" "low-cpu" {
  name                      = "bfd-${var.env_config.env}-${var.role}-low-cpu-policy"
  autoscaling_group_name    = aws_autoscaling_group.main.name
  adjustment_type           = "ChangeInCapacity"
  policy_type               = "StepScaling"
  estimated_instance_warmup = var.asg_config.instance_warmup
  metric_aggregation_type   = "Average"

  step_adjustment {
    scaling_adjustment          = -1
    metric_interval_lower_bound = -10.0
    metric_interval_upper_bound = 0.0
  }

  step_adjustment {
    scaling_adjustment          = -2
    metric_interval_upper_bound = -10.0
  }
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
