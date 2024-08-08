locals {
  enable_rds_scheduled_scaling = !var.disable_rds_scheduling_override && (local.env == "test" || local.is_ephemeral_env)
  replicas_scaling_target      = local.enable_rds_scheduled_scaling ? one(aws_appautoscaling_target.dynamic_replicas) : one(aws_appautoscaling_target.static_replicas)
}

resource "aws_appautoscaling_target" "dynamic_replicas" {
  # All app autoscaling resources need to be applied after the writer exists or read replicas will
  # not automatically be created if the min_capacity is not satisfied
  depends_on = [aws_rds_cluster_instance.writer]

  # only applicable for test environment or ephemeral environments unless override is specified
  count = local.enable_rds_scheduled_scaling ? 1 : 0

  service_namespace  = "rds"
  scalable_dimension = "rds:cluster:ReadReplicaCount"
  resource_id        = "cluster:${aws_rds_cluster.aurora_cluster.id}"

  # NOTE: These are the initial, additional capacity settings for the target. Scaling events by schedule and
  # policy directly update these values on the app autoscaling target. As a result, changes to either
  # min_capacity and max_capacity fields are ignored by the lifecycle meta argument below.
  min_capacity = local.rds_min_reader_nodes
  max_capacity = local.rds_max_reader_nodes

  lifecycle {
    ignore_changes = [
      min_capacity,
      max_capacity
    ]
  }
}

resource "aws_appautoscaling_target" "static_replicas" {
  depends_on = [aws_rds_cluster_instance.writer]

  # only applicable for prod/prod-sbx (not test or ephemeral)
  count = !local.enable_rds_scheduled_scaling ? 1 : 0

  service_namespace  = "rds"
  scalable_dimension = "rds:cluster:ReadReplicaCount"
  resource_id        = "cluster:${aws_rds_cluster.aurora_cluster.id}"

  min_capacity = local.rds_min_reader_nodes
  max_capacity = local.rds_max_reader_nodes
}

resource "aws_appautoscaling_policy" "replicas_cpu_scaling" {
  depends_on = [aws_rds_cluster_instance.writer]

  name               = "bfd-${local.env}-cpu-scaling"
  service_namespace  = local.replicas_scaling_target.service_namespace
  resource_id        = local.replicas_scaling_target.resource_id
  scalable_dimension = local.replicas_scaling_target.scalable_dimension
  policy_type        = "TargetTrackingScaling"

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "RDSReaderAverageCPUUtilization"
    }

    target_value       = local.rds_scaling_cpu_target
    scale_in_cooldown  = local.rds_scale_in_cooldown
    scale_out_cooldown = local.rds_scale_out_cooldown
  }
}

resource "aws_appautoscaling_scheduled_action" "work_hours_scale_out" {
  depends_on = [aws_rds_cluster_instance.writer]

  count = local.enable_rds_scheduled_scaling ? 1 : 0

  name               = "bfd-${local.env}-work-hours-scale-out"
  service_namespace  = local.replicas_scaling_target.service_namespace
  resource_id        = local.replicas_scaling_target.resource_id
  scalable_dimension = local.replicas_scaling_target.scalable_dimension
  schedule           = "cron(00 07 ? * MON-FRI *)"
  timezone           = "America/New_York"

  # NOTE: min_capacity and max_capacity will count nodes that are and
  # are not managed by app-autoscaling when calculating the desired capacity.
  scalable_target_action {
    min_capacity = local.rds_min_reader_nodes
    max_capacity = local.rds_max_reader_nodes
  }
}

resource "aws_appautoscaling_scheduled_action" "off_hours_scale_in" {
  depends_on = [aws_rds_cluster_instance.writer]

  count = local.enable_rds_scheduled_scaling ? 1 : 0

  name               = "bfd-${local.env}-off-hours-scale-in"
  service_namespace  = local.replicas_scaling_target.service_namespace
  resource_id        = local.replicas_scaling_target.resource_id
  scalable_dimension = local.replicas_scaling_target.scalable_dimension
  schedule           = "cron(00 19 ? * MON-FRI *)"
  timezone           = "America/New_York"

  # NOTE: min_capacity and max_capacity only impacts nodes managed by app-autoscaling
  scalable_target_action {
    min_capacity = 0
    max_capacity = 0
  }
}
