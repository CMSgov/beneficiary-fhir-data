locals {
  asgs = {
    odd = {
      name             = "${aws_launch_template.main.name}-odd"
      desired_capacity = local.odd_needs_scale_out ? max(local.even_remote_desired_capacity, var.asg_config.desired) : (local.odd_maintains_state ? local.odd_remote_desired_capacity : 0)
      max_size         = var.asg_config.max
      min_size         = local.odd_needs_scale_out ? max(local.even_remote_desired_capacity, var.asg_config.desired) : (local.odd_maintains_state ? local.odd_remote_desired_capacity : 0)
      warmpool_size    = local.odd_needs_scale_out ? max(local.even_remote_warmpool_min_size, var.asg_config.min) : (local.odd_maintains_state ? local.odd_remote_warmpool_min_size : 0)
    }
    even = {
      name             = "${aws_launch_template.main.name}-even"
      desired_capacity = local.even_needs_scale_out ? max(local.odd_remote_desired_capacity, var.asg_config.desired) : (local.even_maintains_state ? local.even_remote_desired_capacity : 0)
      max_size         = var.asg_config.max
      min_size         = local.even_needs_scale_out ? max(local.odd_remote_desired_capacity, var.asg_config.desired) : (local.even_maintains_state ? local.even_remote_desired_capacity : 0)
      warmpool_size    = local.even_needs_scale_out ? max(local.odd_remote_warmpool_min_size, var.asg_config.min) : (local.even_maintains_state ? local.even_remote_warmpool_min_size : 0)
    }
  }

  env      = terraform.workspace
  seed_env = var.seed_env

  # When the CustomEndpoint is empty, fall back to the ReaderEndpoint
  rds_reader_endpoint = data.external.rds.result["ReaderEndpoint"]

  latest_ltv                     = tonumber(aws_launch_template.main.latest_version)
  is_latest_launch_template_odd  = local.latest_ltv % 2 == 1
  is_latest_launch_template_even = local.latest_ltv % 2 == 0

  odd_remote_desired_capacity  = tonumber(data.external.current_asg.result["odd_desired_capacity"])
  even_remote_desired_capacity = tonumber(data.external.current_asg.result["even_desired_capacity"])

  odd_remote_warmpool_min_size  = tonumber(data.external.current_asg.result["odd_warmpool_min_size"])
  even_remote_warmpool_min_size = tonumber(data.external.current_asg.result["even_warmpool_min_size"])

  #ODD scales OUT when launchtemplate is ODD and ODD ASG has 0 desired capacity
  odd_needs_scale_out = alltrue([
    local.is_latest_launch_template_odd,
    local.odd_remote_desired_capacity == 0
  ])

  #ODD scales IN when launchtemplate EVEN, both ODD and EVEN ASGS have capacity
  odd_needs_scale_in = alltrue([
    local.is_latest_launch_template_even,
    local.odd_remote_desired_capacity > 0,
    local.even_remote_desired_capacity > 0
  ])

  #ODD maintains state when ODD does not need to scale IN or OUT
  odd_maintains_state = alltrue([
    !local.odd_needs_scale_out,
    !local.odd_needs_scale_in
  ])

  #EVEN scales OUT when launchtemplate is EVEN and EVEN ASG has 0 desired capacity
  even_needs_scale_out = alltrue([
    local.is_latest_launch_template_even,
    local.even_remote_desired_capacity == 0
  ])

  #EVEN scales IN when launchtemplate is ODD, both EVEN and ODD ASGs have capacity
  even_needs_scale_in = alltrue([
    local.is_latest_launch_template_odd,
    local.even_remote_desired_capacity > 0,
    local.odd_remote_desired_capacity > 0
  ])

  #EVEN maintains state when EVEN does not need to scale IN or OUT
  even_maintains_state = alltrue([
    !local.even_needs_scale_out,
    !local.even_needs_scale_in
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
      throughput            = var.launch_config.volume_throughput
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

resource "aws_autoscaling_group" "main" {
  # Deployments of this ASG require two executions of `terraform apply`
  for_each = local.asgs

  name             = each.value.name
  desired_capacity = each.value.desired_capacity
  max_size         = each.value.max_size
  min_size         = each.value.min_size

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
    heartbeat_timeout    = var.asg_config.instance_warmup * 2
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

  # NOTE: AWS Provider for Terraform does not fully respect the warm pool blocks
  # - it reports that the warm pool will be destroyed when it is not called for
  # - it reports that the warm pool will be destroyed when we want to destroy it
  # - in either case, terraform 1.5.0 with provider AWS v5.53.0 does not destroy the warm pool
  # See null resource provider below for deletion logic
  force_delete_warm_pool = true
  dynamic "warm_pool" {
    for_each = each.value.warmpool_size == 0 ? toset([]) : toset([each.value.warmpool_size])
    content {
      pool_state                  = "Stopped"
      min_size                    = warm_pool.value
      max_group_prepared_capacity = warm_pool.value
      instance_reuse_policy {
        reuse_on_scale_in = false
      }
    }
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
}

resource "aws_cloudwatch_metric_alarm" "avg_cpu_low" {
  for_each   = local.asgs
  alarm_name = "${each.value.name}-avg-cpu-low"

  comparison_operator = "LessThanThreshold"
  datapoints_to_alarm = local.scaling_alarms_config.scale_in.datapoints_to_alarm
  evaluation_periods  = local.scaling_alarms_config.scale_in.consecutive_periods_to_alarm
  threshold           = local.scale_in_cpu_threshold
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_autoscaling_policy.avg_cpu_low[each.key].arn]

  metric_query {
    id          = "m1"
    return_data = true

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main[each.key].name
      }
      metric_name = "CPUUtilization"
      namespace   = "AWS/EC2"
      period      = local.scaling_alarms_config.scale_in.eval_period
      stat        = "Average"
    }
  }
}

resource "aws_autoscaling_policy" "avg_cpu_low" {
  for_each = local.asgs

  name                      = "${each.value.name}-avg-cpu-low"
  autoscaling_group_name    = aws_autoscaling_group.main[each.key].name
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
  for_each = local.asgs

  alarm_name          = "${each.value.name}-avg-cpu-high"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  datapoints_to_alarm = local.scaling_alarms_config.scale_out.datapoints_to_alarm
  evaluation_periods  = local.scaling_alarms_config.scale_out.consecutive_periods_to_alarm
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_autoscaling_policy.avg_cpu_high[each.key].arn]

  metric_query {
    id          = "m1"
    return_data = false

    metric {
      dimensions = {
        AutoScalingGroupName = aws_autoscaling_group.main[each.key].name
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
        AutoScalingGroupName = aws_autoscaling_group.main[each.key].name
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
        AutoScalingGroupName = aws_autoscaling_group.main[each.key].name
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
  for_each = local.asgs

  name                      = "${each.value.name}-avg-cpu-high"
  autoscaling_group_name    = aws_autoscaling_group.main[each.key].name
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

# NOTE: Required to *actually* delete a warm pool as of terraform 1.5.0 and AWS Provider v5.53.0
resource "null_resource" "warm_pool_size" {
  for_each = local.asgs

  triggers = {
    should_delete_warmpool = each.value.warmpool_size == 0
  }

  provisioner "local-exec" {
    environment = {
      will_delete = self.triggers.should_delete_warmpool
      asg_name    = each.value.name
    }

    command = "[[ $will_delete == \"true\" ]] && aws autoscaling delete-warm-pool --auto-scaling-group-name \"$asg_name\" --force-delete || echo \"No change to $asg_name will_delete = $will_delete\""
  }
}
