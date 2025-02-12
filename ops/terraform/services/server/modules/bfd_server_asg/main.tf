locals {
  green_state = "green"
  blue_state  = "blue"
  asgs = {
    odd = {
      name              = "${aws_launch_template.main.name}-odd"
      lt_version        = local.is_latest_launch_template_odd ? local.latest_ltv : max(local.latest_ltv - 1, 1)
      desired_capacity  = local.odd_needs_scale_out ? max(local.even_remote_desired_capacity, var.asg_config.desired) : (local.odd_maintains_state ? local.odd_remote_desired_capacity : 0)
      max_size          = var.asg_config.max
      min_size          = local.odd_needs_scale_out ? max(local.even_remote_desired_capacity, var.asg_config.desired) : (local.odd_maintains_state ? local.odd_remote_desired_capacity : 0)
      warmpool_size     = local.odd_needs_scale_out ? max(local.even_remote_warmpool_min_size, var.asg_config.min) : (local.odd_maintains_state ? local.odd_remote_warmpool_min_size : 0)
      deployment_status = local.odd_needs_scale_out ? local.green_state : (local.odd_maintains_state ? local.blue_state : local.green_state)
    }
    even = {
      name              = "${aws_launch_template.main.name}-even"
      lt_version        = local.is_latest_launch_template_even ? local.latest_ltv : max(local.latest_ltv - 1, 1)
      desired_capacity  = local.even_needs_scale_out ? max(local.odd_remote_desired_capacity, var.asg_config.desired) : (local.even_maintains_state ? local.even_remote_desired_capacity : 0)
      max_size          = var.asg_config.max
      min_size          = local.even_needs_scale_out ? max(local.odd_remote_desired_capacity, var.asg_config.desired) : (local.even_maintains_state ? local.even_remote_desired_capacity : 0)
      warmpool_size     = local.even_needs_scale_out ? max(local.odd_remote_warmpool_min_size, var.asg_config.min) : (local.even_maintains_state ? local.even_remote_warmpool_min_size : 0)
      deployment_status = local.even_needs_scale_out ? local.green_state : (local.even_maintains_state ? local.blue_state : local.green_state)
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

  # We need to know when an operator is attempting to create a new Launch Template version (most
  # likely with a new AMI) when both odd and even are scaled-out. This is an invalid state, and
  # likely indicates that the operator is attempting to resolve the situation where "green"
  # instances failed the Regression Suite or other verification before being promoted to "blue"
  # instances. Because "green" and "blue" changes based on Launch Template version divisiblity, the
  # operator must resolve this situation out-of-band (likely by incrementing the LT version and then
  # resetting the previous green ASG) before applying.
  prev_green_needs_reset = alltrue([
    # ODD is scaled-out
    local.odd_remote_desired_capacity > 0,
    # EVEN is scaled-out
    local.even_remote_desired_capacity > 0,
    # But, the existing, latest launch template is being replaced by a new Template (likely with a
    # new AMI) indicating that an operator is attempting to create new instances in-between green
    # (the incoming version) being accepted into blue
    data.external.current_lt_version.result["latest_version"] != tostring(aws_launch_template.main.latest_version)
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

  dynamic "ingress" {
    for_each = var.lb_config.target_group_config
    content {
      from_port       = ingress.value.port
      to_port         = ingress.value.port
      protocol        = ingress.value.protocol
      security_groups = [aws_security_group.lb.id]
    }
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

# These AutoScaling Groups require 2 external "null_resourece" resources to manage their Target
# Group ARNs and their Warm Pool due to defective behavior in the AWS provider. Specifically,
# attempting to modify Target Group ARNs dynamically (to do blue/green) results in an error during
# apply with a message indicating a bug in the provider. Additionally, attempting to manage the Warm
# Pool via Terraform results in an erreneous 3 minute delay upon destruction of the Warm Pool, even
# with "force_delete_warm_pool" enabled, which is unnaceptable
resource "aws_autoscaling_group" "main" {
  # Deployments of this ASG require two executions of `terraform apply`.
  for_each = local.asgs

  lifecycle {
    precondition {
      condition     = local.prev_green_needs_reset == false
      error_message = <<-EOF
A new launch template cannot be created while both ASGs (blue and green) are scaled-out. This
situation typically indicates that green, the incoming ASG, failed automated checks. Refer to the
corresponding Runbook for instructions on how to remediate this situation.
EOF
    }

    # For "target_group_arns" look at "null_resource.set_target_groups" as Terraform's AWS Provider
    # has a bug in it when changing target group ARNs in the terraform. For "warm_pool" look at
    # "null_resource.manage_warm_pool" as Terraform's AWS Provider does not respect
    # "force_delete_warm_pool" which would result in non-zero downtime deployments as scaling-in the
    # previous "blue" blocks incoming "blue" from being attached to the "blue" Target Group
    ignore_changes = [target_group_arns, warm_pool]
  }

  # TODO: Remove below code in BFD-3878
  load_balancers = var.legacy_clb_name != null ? [var.legacy_clb_name] : null
  # TODO: Remove above code in BFD-3878

  name                      = each.value.name
  desired_capacity          = each.value.desired_capacity
  max_size                  = each.value.max_size
  min_size                  = each.value.min_size
  wait_for_capacity_timeout = var.lb_config == null ? null : "20m"
  health_check_grace_period = 600                                   # Temporary, will be lowered when/if lifecycle hooks are implemented
  health_check_type         = var.lb_config == null ? "EC2" : "ELB" # Failures of ELB healthchecks are asg failures
  vpc_zone_identifier       = data.aws_subnet.app_subnets[*].id
  termination_policies      = ["OldestLaunchTemplate"]

  # With Lifecycle Hooks, BFD Server instances will not reach the InService state until the BFD
  # Server application running on the instance is ready to start serving traffic. As a result, it is
  # unnecessary to have any instance warmup period
  default_instance_warmup = 0

  launch_template {
    name    = aws_launch_template.main.name
    version = each.value.lt_version
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

resource "null_resource" "set_target_groups" {
  for_each = local.asgs

  triggers = {
    target_group_name = each.value.deployment_status
    desired_capacity  = each.value.desired_capacity
  }

  provisioner "local-exec" {
    environment = {
      asg_name          = aws_autoscaling_group.main[each.key].name
      desired_capacity  = self.triggers.desired_capacity
      target_group_arn  = aws_lb_target_group.main[self.triggers.target_group_name].arn
      target_group_name = self.triggers.target_group_name
    }

    command = <<-EOF
attached_other_tgs="$(
  aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names "$asg_name" |
    jq -r --arg target_group_arn "$target_group_arn" \
      '.AutoScalingGroups[0].TargetGroupARNs | map(select(. != $target_group_arn)) | join(",")'
)"
if [[ -n "$attached_other_tgs" ]]; then
  aws autoscaling detach-load-balancer-target-groups \
    --auto-scaling-group-name "$asg_name" \
    --target-group-arns "$attached_other_tgs"
  echo "Detached $asg_name from all non-$target_group_name Target Groups"
fi
if ((desired_capacity > 0)); then
  aws autoscaling attach-load-balancer-target-groups \
    --auto-scaling-group-name "$asg_name" \
    --target-group-arns "$target_group_arn" &&
    echo "Attached $asg_name to $target_group_name Target Group"
fi
EOF
  }
}

# NOTE: Required to avoid erroneous 3 minute delay when scaling-in an ASG's Warm Pool due to defects
# in the Terraform AWS Provider as of Terraform 1.5.0 and version 5.53.0 of the AWS Provider
resource "null_resource" "manage_warm_pool" {
  for_each = local.asgs

  triggers = {
    warmpool_size = each.value.warmpool_size
  }

  provisioner "local-exec" {
    environment = {
      warmpool_size = self.triggers.warmpool_size
      asg_name      = aws_autoscaling_group.main[each.key].name
    }

    command = <<-EOF
if ((warmpool_size == 0)); then
  aws autoscaling delete-warm-pool --auto-scaling-group-name "$asg_name" --force-delete
  echo "Deleting "$asg_name" warm_pool. 'warmpool_size' is 0"
else
  aws autoscaling put-warm-pool --auto-scaling-group-name "$asg_name" \
    --max-group-prepared-capacity "$warmpool_size" \
    --min-size "$warmpool_size" \
    --pool-state "Stopped" \
    --instance-reuse-policy "ReuseOnScaleIn=false"
  echo "Creating Warm Pool for "$asg_name" with size of $warmpool_size"
fi
EOF
  }
}

### Load Balancer Components ###
resource "aws_lb" "main" {
  name                             = var.lb_config.name
  internal                         = coalesce(var.lb_config.internal, true)
  load_balancer_type               = var.lb_config.load_balancer_type
  security_groups                  = [aws_security_group.lb.id]
  subnets                          = data.aws_subnet.app_subnets[*].id # Gives AZs and VPC association
  enable_deletion_protection       = coalesce(var.lb_config.enable_deletion_protection, false)
  client_keep_alive                = coalesce(var.lb_config.client_keep_alive_seconds, 3600)
  idle_timeout                     = coalesce(var.lb_config.idle_timeout_seconds, 60)
  ip_address_type                  = coalesce(var.lb_config.ip_address_type, "ipv4")
  enable_http2                     = coalesce(var.lb_config.enable_http2, "false")
  desync_mitigation_mode           = coalesce(var.lb_config.desync_mitigation_mode, "strictest")
  enable_cross_zone_load_balancing = coalesce(var.lb_config.enable_cross_zone_load_balancing, "true")

  tags = local.additional_tags
}

resource "aws_lb_listener" "main" {
  for_each = { for config in var.lb_config.load_balancer_listener_config : config.id => config }

  load_balancer_arn = aws_lb.main.arn
  port              = each.value.port
  protocol          = each.value.protocol

  default_action {
    type             = each.value.default_action_type
    target_group_arn = aws_lb_target_group.main[each.key].arn
  }
}

# security group
resource "aws_security_group" "lb" {
  lifecycle {
    create_before_destroy = true
  }

  name        = "${var.lb_config.name}-sg"
  description = "Allow access to the ${var.role} load-balancer"
  vpc_id      = var.env_config.vpc_id
  tags        = merge({ Name = "${var.lb_config.name}-sg" }, local.additional_tags)

  # Dynamic, per-listener CIDR Block ingress rules
  dynamic "ingress" {
    for_each = var.lb_config.load_balancer_listener_config
    content {
      from_port   = ingress.value.port
      to_port     = ingress.value.port
      protocol    = ingress.value.protocol
      cidr_blocks = var.lb_config.load_balancer_security_group_config.ingress.cidr_blocks
      description = var.lb_config.load_balancer_security_group_config.ingress.description
    }
  }

  # Dynamic, per-listener prefix list ingress rules if prefix list IDs are specified
  dynamic "ingress" {
    for_each = length(var.lb_config.load_balancer_security_group_config.ingress.prefix_list_ids) > 0 ? var.lb_config.load_balancer_listener_config : []
    content {
      from_port       = ingress.value.port
      to_port         = ingress.value.port
      protocol        = ingress.value.protocol
      prefix_list_ids = var.lb_config.load_balancer_security_group_config.ingress.prefix_list_ids
      description     = var.lb_config.load_balancer_security_group_config.ingress.description
    }
  }

  dynamic "egress" {
    for_each = var.lb_config.target_group_config
    content {
      from_port   = egress.value.port
      to_port     = egress.value.port
      protocol    = egress.value.protocol
      cidr_blocks = var.lb_config.load_balancer_security_group_config.egress.cidr_blocks
      description = var.lb_config.load_balancer_security_group_config.egress.description
    }
  }
}

resource "aws_lb_target_group" "main" {
  lifecycle {
    create_before_destroy = true
  }

  for_each = { for config in var.lb_config.target_group_config : config.id => config }

  name                   = each.value.name
  port                   = each.value.port
  protocol               = each.value.protocol
  vpc_id                 = var.env_config.vpc_id
  deregistration_delay   = each.value.deregisteration_delay_seconds
  connection_termination = true
  health_check {
    healthy_threshold   = each.value.health_check_config.healthy_threshold
    interval            = each.value.health_check_config.health_check_interval_seconds
    timeout             = each.value.health_check_config.health_check_timeout_seconds
    unhealthy_threshold = each.value.health_check_config.unhealthy_threshold
    port                = 7443
    protocol            = "TCP"
  }
}
