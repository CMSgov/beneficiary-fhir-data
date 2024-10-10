locals {
  env      = terraform.workspace
  seed_env = var.seed_env

  # When the CustomEndpoint is empty, fall back to the ReaderEndpoint
  rds_reader_endpoint = data.external.rds.result["ReaderEndpoint"]

  # These variables ensures that we accommodate deployments where the ASG is already scaled-out
  asg_instance_count       = tonumber(data.external.current_asg_instances.result["count"])
  dynamic_desired_capacity = max(length([for each in jsondecode(data.external.current_asg_instances.result["instance_launch_templates"]) : true if tonumber(each) != tonumber(aws_launch_template.main.latest_version)]), var.asg_config.desired)

  # Dynamic capacity used here to avoid scaling out after ASG was unable to satisfy previous
  # requested ASG settings, e.g. partial/total deployment failures, other ASG capacity issues
  not_initial_deployment             = local.asg_instance_count >= local.dynamic_desired_capacity
  is_old_version_present_in_asg      = alltrue([for each in jsondecode(data.external.current_asg_instances.result["instance_launch_templates"]) : true if tonumber(each) != tonumber(aws_launch_template.main.latest_version)])
  is_current_version_absent_from_asg = alltrue([for each in jsondecode(data.external.current_asg_instances.result["instance_launch_templates"]) : false if tonumber(each) == tonumber(aws_launch_template.main.latest_version)])

  # Scale out when all of these statements are true
  # - the ASG already existed before running this terraform apply
  # - the ASG contains instances running an outdated launch template version
  # - the ASG does *not* contain instances running the latest launch template version
  need_scale_out = alltrue([
    local.not_initial_deployment,
    local.is_old_version_present_in_asg,
    local.is_current_version_absent_from_asg
  ])

  additional_tags = { Layer = var.layer, role = var.role }

  # FUTURE: Encode the scaling step, scaling stages, and alarm evaluation periods within config
  scaling_capacity_step = length(var.env_config.azs)
  scaling_alarms_config = {
    # Scale-out alarm is configured to ALARM if 1 out of 1 consecutive 1 minute periods report high
    # network traffic. This is intended to scale the Server aggressively
    scale_out = {
      eval_period                  = 1 * 60
      datapoints_to_alarm          = 1
      consecutive_periods_to_alarm = 1
    }
    # Scale-in alarms are configured to ALARM if 10 out of 10 consecutive 1 minute periods report
    # low network traffic. This is intended to keep the Server scaled-out as traffic is often bursty
    # over a given period rather than sustained at a given threshold
    scale_in = {
      eval_period                  = 1 * 60
      datapoints_to_alarm          = 10
      consecutive_periods_to_alarm = 10
    }
  }
  scale_in_cpu_threshold = 15 # 15 percent average cpu for above period will cause scale-in
  # 3 ranges of CPU utilization to scale out at, with each range intended to _increase_ the scaling
  # response when average CPU utilization is increased. For example, if CPU utilization hits 90%
  # over the evaluation period of scale-out at 3 instances, we want to scale-out to 12 instances
  # immediately
  scale_out_cpu_thresholds = [
    { begin = 50, end = 75 },
    { begin = 75, end = 90 },
    { begin = 90, end = null }
  ]

  on_launch_lifecycle_hook_name = "bfd-${local.env}-${var.role}-on-launch"

  # Adds the ApplicationName to the JDBC connection indicating that queries are from the server.
  full_jdbc_suffix = "${var.jdbc_suffix}&ApplicationName=bfd-server"
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
  for_each    = var.db_config != null ? toset(var.db_config.db_sg) : []
  type        = "ingress"
  from_port   = 5432
  to_port     = 5432
  protocol    = "tcp"
  description = "Allows access to the ${var.db_config.role} db"

  security_group_id        = each.value                   # The SG associated with each replica
  source_security_group_id = aws_security_group.app[0].id # Every instance in the ASG
}


## Launch Template
resource "aws_launch_template" "main" {
  name                   = "bfd-${local.env}-${var.role}"
  description            = "Template for the ${local.env} environment ${var.role} servers"
  vpc_security_group_ids = concat([aws_security_group.base.id, var.mgmt_config.vpn_sg, var.mgmt_config.tool_sg], aws_security_group.app[*].id)
  key_name               = var.launch_config.key_name
  image_id               = var.launch_config.ami_id
  instance_type          = var.launch_config.instance_type
  ebs_optimized          = true
  update_default_version = true

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
      delete_on_termination = true
      encrypted             = true
      iops                  = var.launch_config.volume_iops
      kms_key_id            = data.aws_kms_key.master_key.arn
      throughput            = 250 #FIXME var.launch_config.volume_throughput
      volume_size           = var.launch_config.volume_size
      volume_type           = var.launch_config.volume_type
    }
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 1
    http_tokens                 = "required"
  }

  user_data = base64encode(templatefile("${path.module}/templates/${var.launch_config.user_data_tpl}", {
    env                   = local.env
    seed_env              = local.seed_env
    port                  = var.lb_config.port
    accountId             = var.launch_config.account_id
    reader_endpoint       = "jdbc:postgresql://${local.rds_reader_endpoint}:5432/fhirdb${local.full_jdbc_suffix}"
    launch_lifecycle_hook = local.on_launch_lifecycle_hook_name
  }))

  tag_specifications {
    resource_type = "instance"
    tags          = merge({ Name = "bfd-${local.env}-${var.role}" }, local.additional_tags)
  }

  tag_specifications {
    resource_type = "volume"
    tags          = merge({ Name = "bfd-${local.env}-${var.role}" }, local.additional_tags)
  }
}


## Autoscaling group
resource "aws_autoscaling_group" "main" {
  # Deployments of this ASG require two executions of `terraform apply` where:
  # 1. request instances running the latest launch template versions by scale-out
  # 2. request instances running outdated launch teamplate versions be destroyed by scale-in
  name             = aws_launch_template.main.name
  desired_capacity = local.need_scale_out ? max(2 * local.asg_instance_count, 2 * var.asg_config.desired) : local.dynamic_desired_capacity
  max_size         = local.need_scale_out ? min(4 * local.asg_instance_count, 2 * var.asg_config.max) : var.asg_config.max
  min_size         = local.need_scale_out ? max(2 * local.asg_instance_count, 2 * var.asg_config.min) : var.asg_config.min

  # If an lb is defined, wait for the ELB
  min_elb_capacity          = var.lb_config == null ? null : var.asg_config.min
  wait_for_capacity_timeout = var.lb_config == null ? null : "20m"

  health_check_grace_period = 600                                   # Temporary, will be lowered when/if lifecycle hooks are implemented
  health_check_type         = var.lb_config == null ? "EC2" : "ELB" # Failures of ELB healthchecks are asg failures
  vpc_zone_identifier       = data.aws_subnet.app_subnets[*].id
  load_balancers            = var.lb_config == null ? [] : [var.lb_config.name]
  termination_policies      = ["OldestLaunchTemplate"]

  # With Lifecycle Hooks, BFD Server instances will not reach the InService state until the BFD
  # Server application running on the instance is ready to start serving traffic. As a result, it is
  # unnecessary to have any cooldown or instance warmup period
  default_cooldown        = 0
  default_instance_warmup = 0

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

resource "aws_cloudwatch_metric_alarm" "avg_cpu_low" {
  alarm_name          = "bfd-${var.role}-${local.env}-avg-cpu-low"
  comparison_operator = "LessThanThreshold"
  datapoints_to_alarm = local.scaling_alarms_config.scale_in.datapoints_to_alarm
  evaluation_periods  = local.scaling_alarms_config.scale_in.consecutive_periods_to_alarm
  threshold           = local.scale_in_cpu_threshold
  treat_missing_data  = "notBreaching"
  alarm_actions       = local.need_scale_out ? [] : [aws_autoscaling_policy.avg_cpu_low.arn]

  metric_query {
    id          = "m1"
    return_data = true

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "CPUUtilization"
      namespace   = "AWS/EC2"
      period      = local.scaling_alarms_config.scale_in.eval_period
      stat        = "Average"
    }
  }
}

resource "aws_autoscaling_policy" "avg_cpu_low" {
  name                      = "bfd-${var.role}-${local.env}-avg-cpu-low"
  autoscaling_group_name    = aws_autoscaling_group.main.name
  estimated_instance_warmup = 0 # explicitly disable warmup as lifecycle hooks handle it more accurately
  adjustment_type           = "ChangeInCapacity"
  metric_aggregation_type   = "Average"
  policy_type               = "StepScaling"

  step_adjustment {
    metric_interval_upper_bound = 0
    scaling_adjustment          = "-${local.scaling_capacity_step}"
  }
}

resource "aws_cloudwatch_metric_alarm" "avg_cpu_high" {
  alarm_name          = "bfd-${var.role}-${local.env}-avg-cpu-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  datapoints_to_alarm = local.scaling_alarms_config.scale_out.datapoints_to_alarm
  evaluation_periods  = local.scaling_alarms_config.scale_out.consecutive_periods_to_alarm
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_autoscaling_policy.avg_cpu_high.arn]

  metric_query {
    id          = "m1"
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "CPUUtilization"
      namespace   = "AWS/EC2"
      period      = local.scaling_alarms_config.scale_out.eval_period
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
      metric_name = "GroupDesiredCapacity"
      namespace   = "AWS/AutoScaling"
      period      = local.scaling_alarms_config.scale_out.eval_period
      stat        = "Average"
    }
  }

  metric_query {
    id          = "m3"
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main.name
      }
      metric_name = "GroupInServiceInstances"
      namespace   = "AWS/AutoScaling"
      period      = local.scaling_alarms_config.scale_out.eval_period
      stat        = "Average"
    }
  }

  # This is a bit complex, but in short this creates an IF() metric math for each defined range that
  # will scale proportionally to the incoming load. However, because of the quirks of default Step
  # Scaling policies, these generated expressions guard against additional, erroneous scaling by
  # ensuring these ranges are only evaluated when the desired capacity matches the existing
  # capacity. Otherwise, normal Step Scaling policies will continue to scale indefinitely (until the
  # maximum) as instances take too long to warmup and contribute to aggregated CPU metrics
  dynamic "metric_query" {
    for_each = local.scale_out_cpu_thresholds
    content {
      id    = "e${metric_query.key}"
      label = "Scaling Capacity Scalar (DesiredCapacity = (${local.scaling_capacity_step} + (${local.scaling_capacity_step} * SCS)))"
      expression = "IF(${join(" && ", compact([
        metric_query.value.begin != null ? "m1 > ${metric_query.value.begin}" : null,
        metric_query.value.end != null ? "m1 <= ${metric_query.value.end}" : null,
        "m2 == m3"
      ]))}, MIN([m3/${local.scaling_capacity_step} + ${metric_query.key}, TIME_SERIES(${length(local.scale_out_cpu_thresholds)})]), 0)"
      return_data = false
    }
  }

  metric_query {
    expression  = "MAX([${join(",", [for i in range(length(local.scale_out_cpu_thresholds)) : "e${i}"])}])"
    id          = "e${length(local.scale_out_cpu_thresholds)}"
    label       = "ScalingCapacityScalar"
    return_data = true
  }
}

resource "aws_autoscaling_policy" "avg_cpu_high" {
  name                      = "bfd-${var.role}-${local.env}-avg-cpu-high"
  autoscaling_group_name    = aws_autoscaling_group.main.name
  estimated_instance_warmup = 0 # explicitly disable warmup as lifecycle hooks handle it more accurately
  adjustment_type           = "ExactCapacity"
  metric_aggregation_type   = "Average"
  policy_type               = "StepScaling"

  dynamic "step_adjustment" {
    for_each = local.scale_out_cpu_thresholds
    content {
      metric_interval_lower_bound = step_adjustment.key
      metric_interval_upper_bound = step_adjustment.key + 1 != length(local.scale_out_cpu_thresholds) ? step_adjustment.key + 1 : null
      scaling_adjustment          = local.scaling_capacity_step + (local.scaling_capacity_step * (step_adjustment.key + 1))
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
