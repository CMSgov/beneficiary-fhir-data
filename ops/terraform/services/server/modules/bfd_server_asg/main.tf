locals {
  env = terraform.workspace

  # When the CustomEndpoint is empty, fall back to the ReaderEndpoint
  rds_reader_endpoint = data.external.rds.result["CustomEndpoint"] == "" ? data.external.rds.result["ReaderEndpoint"] : data.external.rds.result["CustomEndpoint"]

  additional_tags = { Layer = var.layer, role = var.role }

  scaling_capacity_step = length(var.env_config.azs)
  scaling_stages = [
    { begin = 0, end = 1 * var.scaling_networkin_interval_mb, desired_capacity = local.scaling_capacity_step * 1 },
    { begin = 1 * var.scaling_networkin_interval_mb, end = 2 * var.scaling_networkin_interval_mb, desired_capacity = local.scaling_capacity_step * 2 },
    { begin = 2 * var.scaling_networkin_interval_mb, end = 4 * var.scaling_networkin_interval_mb, desired_capacity = local.scaling_capacity_step * 3 },
    { begin = 4 * var.scaling_networkin_interval_mb, end = null, desired_capacity = local.scaling_capacity_step * 4 },
  ]
  scalein_config  = slice(local.scaling_stages, 0, length(local.scaling_stages) - 1)
  scaleout_config = slice(local.scaling_stages, 1, length(local.scaling_stages))

  on_launch_lifecycle_hook_name = "bfd-${local.env}-${var.role}-on-launch"
}

## Security groups
#

# base
resource "aws_security_group" "base" {
  name        = "bfd-${local.env}-${var.role}-base"
  description = "Allow CI access to app servers"
  vpc_id      = var.env_config.vpc_id
  tags        = merge({ Name = "bfd-${local.env}-${var.role}-base" }, local.additional_tags)

  ingress = [] # Make the ingress empty for this SG.

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# app server
resource "aws_security_group" "app" {
  count       = var.lb_config == null ? 0 : 1
  name        = "bfd-${local.env}-${var.role}-app"
  description = "Allow access to app servers"
  vpc_id      = var.env_config.vpc_id
  tags        = merge({ Name = "bfd-${local.env}-${var.role}-app" }, local.additional_tags)

  ingress {
    from_port       = var.lb_config.port
    to_port         = var.lb_config.port
    protocol        = "tcp"
    security_groups = [var.lb_config.sg]
  }
}

# database
resource "aws_security_group_rule" "allow_db_access" {
  count       = var.db_config == null ? 0 : 1
  type        = "ingress"
  from_port   = 5432
  to_port     = 5432
  protocol    = "tcp"
  description = "Allows access to the ${var.db_config.role} db"

  security_group_id        = var.db_config.db_sg          # The SG associated with each replica
  source_security_group_id = aws_security_group.app[0].id # Every instance in the ASG
}


## Launch Template
#
resource "aws_launch_template" "main" {
  name                   = "bfd-${local.env}-${var.role}"
  description            = "Template for the ${local.env} environment ${var.role} servers"
  vpc_security_group_ids = concat([aws_security_group.base.id, var.mgmt_config.vpn_sg, var.mgmt_config.tool_sg], aws_security_group.app[*].id)
  key_name               = var.launch_config.key_name
  image_id               = var.launch_config.ami_id
  instance_type          = var.launch_config.instance_type
  ebs_optimized          = true

  iam_instance_profile {
    name = var.launch_config.profile
  }

  placement {
    tenancy = "default"
  }

  monitoring {
    enabled = true
  }

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_type           = "gp2"
      volume_size           = var.launch_config.volume_size
      delete_on_termination = true
      encrypted             = true
      kms_key_id            = data.aws_kms_key.master_key.arn
    }
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 1
    http_tokens                 = "required"
  }

  user_data = base64encode(templatefile("${path.module}/templates/${var.launch_config.user_data_tpl}", {
    env                   = local.env
    port                  = var.lb_config.port
    accountId             = var.launch_config.account_id
    data_server_db_url    = "jdbc:postgresql://${local.rds_reader_endpoint}:5432/fhirdb${var.jdbc_suffix}"
    launch_lifecycle_hook = local.on_launch_lifecycle_hook_name
  }))

  tag_specifications {
    resource_type = "instance"
    tags          = merge({ Name = "bfd-${local.env}-${var.role}" }, local.additional_tags)
  }

  tag_specifications {
    resource_type = "volume"
    tags          = merge({ snapshot = "true", Name = "bfd-${local.env}-${var.role}" }, local.additional_tags)
  }
}


## Autoscaling group
#
resource "aws_autoscaling_group" "main" {
  # Generate a new group on every revision of the launch template.
  # This does a simple version of a blue/green deployment
  name             = "${aws_launch_template.main.name}-${aws_launch_template.main.latest_version}"
  desired_capacity = var.asg_config.desired
  max_size         = var.asg_config.max
  min_size         = var.asg_config.min

  # If an lb is defined, wait for the ELB
  min_elb_capacity          = var.lb_config == null ? null : var.asg_config.min
  wait_for_capacity_timeout = var.lb_config == null ? null : "20m"

  health_check_grace_period = 600                                   # Temporary, will be lowered when/if lifecycle hooks are implemented
  health_check_type         = var.lb_config == null ? "EC2" : "ELB" # Failures of ELB healthchecks are asg failures
  vpc_zone_identifier       = data.aws_subnet.app_subnets[*].id
  load_balancers            = var.lb_config == null ? [] : [var.lb_config.name]

  launch_template {
    name    = aws_launch_template.main.name
    version = aws_launch_template.main.latest_version
  }

  initial_lifecycle_hook {
    name                 = local.on_launch_lifecycle_hook_name
    default_result       = "ABANDON"
    heartbeat_timeout    = var.asg_config.instance_warmup * 3
    lifecycle_transition = "autoscaling:EC2_INSTANCE_LAUNCHING"
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

  warm_pool {
    pool_state                  = "Stopped"
    min_size                    = var.asg_config.min
    max_group_prepared_capacity = var.asg_config.max_warm
  }

  dynamic "tag" {
    for_each = merge(local.additional_tags, var.env_config.default_tags)
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }

  tag {
    key                 = "Name"
    value               = "bfd-${local.env}-${var.role}"
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = true
  }
}


## Autoscaling Policies and Cloudwatch Alarms
#

resource "aws_cloudwatch_metric_alarm" "filtered_networkin_low" {
  for_each = { for v in local.scalein_config : "${v.desired_capacity}-instances" => v }

  alarm_name          = "bfd-${var.role}-${local.env}-networkin-low-${each.key}"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  datapoints_to_alarm = 10
  evaluation_periods  = 10
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_autoscaling_policy.filtered_networkin_low_scaling[each.key].arn]

  metric_query {
    id          = "m1"
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "NetworkIn"
      namespace   = "AWS/EC2"
      period      = 60
      stat        = "Average"
    }
  }

  metric_query {
    id          = "m2"
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "NetworkOut"
      namespace   = "AWS/EC2"
      period      = 60
      stat        = "Average"
    }
  }

  metric_query {
    id          = "m3"
    period      = 0
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "GroupDesiredCapacity"
      namespace   = "AWS/AutoScaling"
      period      = 60
      stat        = "Average"
    }
  }

  metric_query {
    expression  = "IF(m2/m1 > 0.01, m1, 0)"
    id          = "networkin"
    label       = "FilteredNetworkIn"
    return_data = false
  }

  metric_query {
    id    = "e2"
    label = "Set to ${each.value.desired_capacity} capacity units"
    expression = "IF(${join(" && ", compact([
      each.value.begin > 0 ? "networkin > ${each.value.begin}" : null,
      each.value.end != null ? "networkin <= ${each.value.end}" : null,
      "m3 > ${each.value.desired_capacity}"
    ]))}, 1)"
    return_data = true
  }
}

resource "aws_autoscaling_policy" "filtered_networkin_low_scaling" {
  for_each = { for v in local.scalein_config : "${v.desired_capacity}-instances" => v }

  name                    = "bfd-${var.role}-${local.env}-networkin-low-scalein-${each.key}"
  autoscaling_group_name  = aws_autoscaling_group.main.name
  adjustment_type         = "ExactCapacity"
  metric_aggregation_type = "Average"
  policy_type             = "StepScaling"

  step_adjustment {
    metric_interval_lower_bound = 0
    scaling_adjustment          = each.value.desired_capacity
  }
}

resource "aws_cloudwatch_metric_alarm" "filtered_networkin_high" {
  alarm_name          = "bfd-${var.role}-${local.env}-networkin-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  datapoints_to_alarm = 1
  evaluation_periods  = 1
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_autoscaling_policy.filtered_networkin_high_scaling.arn]

  metric_query {
    id          = "m1"
    period      = 0
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "NetworkIn"
      namespace   = "AWS/EC2"
      period      = 60
      stat        = "Average"
    }
  }
  metric_query {
    id          = "m2"
    period      = 0
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "NetworkOut"
      namespace   = "AWS/EC2"
      period      = 60
      stat        = "Average"
    }
  }

  metric_query {
    id          = "m3"
    period      = 0
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "GroupDesiredCapacity"
      namespace   = "AWS/AutoScaling"
      period      = 60
      stat        = "Average"
    }
  }

  metric_query {
    expression  = "IF(m2/m1 > 0.01, m1, 0)"
    id          = "networkin"
    label       = "FilteredNetworkIn"
    period      = 0
    return_data = false
  }

  dynamic "metric_query" {
    for_each = local.scaleout_config
    content {
      id    = "e${metric_query.key}"
      label = "Set to ${metric_query.value.desired_capacity} capacity units"
      expression = "IF(${join(" && ", compact([
        "networkin > ${metric_query.value.begin}",
        metric_query.value.end != null ? "networkin <= ${metric_query.value.end}" : null,
        "m3 < ${metric_query.value.desired_capacity}"
      ]))}, ${metric_query.key + 1})"
      return_data = false
    }
  }

  metric_query {
    expression  = "MAX([${join(",", [for i in range(length(local.scaleout_config)) : "e${i}"])}])"
    id          = "e${length(local.scaleout_config)}"
    label       = "ScalingCapacityScalar"
    period      = 0
    return_data = true
  }
}

resource "aws_autoscaling_policy" "filtered_networkin_high_scaling" {
  name                    = "bfd-${var.role}-${local.env}-networkin-high-scaleout"
  autoscaling_group_name  = aws_autoscaling_group.main.name
  adjustment_type         = "ExactCapacity"
  metric_aggregation_type = "Average"
  policy_type             = "StepScaling"

  dynamic "step_adjustment" {
    for_each = local.scaleout_config
    content {
      metric_interval_lower_bound = step_adjustment.key
      metric_interval_upper_bound = step_adjustment.key + 1 != length(local.scaleout_config) ? step_adjustment.key + 1 : null
      scaling_adjustment          = step_adjustment.value.desired_capacity
    }
  }
}

## Autoscaling Notifications
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
