resource "aws_appautoscaling_target" "replicas" {
  # only applicable for test environment
  count = local.env == "test" ? 1 : 0

  service_namespace  = "rds"
  scalable_dimension = "rds:cluster:ReadReplicaCount"
  resource_id        = "cluster:${aws_rds_cluster.aurora_cluster.id}"

  # NOTE: These are the initial, additional capacity settings for the target. Scaling events by schedule and
  # policy directly update these values on the app autoscaling target. As a result, changes to either
  # min_capacity and max_capacity fields are ignored by the lifecycle meta argument below.
  min_capacity = 0
  max_capacity = 0

  lifecycle {
    ignore_changes = [
      min_capacity,
      max_capacity
    ]
  }
}

resource "aws_appautoscaling_scheduled_action" "scale_out" {
  count = local.env == "test" ? 1 : 0

  name               = "bfd-${local.env}-scale-out"
  service_namespace  = aws_appautoscaling_target.replicas[0].service_namespace
  resource_id        = aws_appautoscaling_target.replicas[0].resource_id
  scalable_dimension = aws_appautoscaling_target.replicas[0].scalable_dimension
  schedule           = "cron(00 07 ? * MON-FRI *)"
  timezone           = "America/New_York"

  # NOTE: min_capacity and max_capacity will count nodes that are and
  # are not managed by app-autoscaling when calculating the desired capacity.
  scalable_target_action {
    min_capacity = 1
    max_capacity = 1
  }
}

resource "aws_appautoscaling_scheduled_action" "scale_in" {
  count = local.env == "test" ? 1 : 0

  name               = "bfd-${local.env}-scale-in"
  service_namespace  = aws_appautoscaling_target.replicas[0].service_namespace
  resource_id        = aws_appautoscaling_target.replicas[0].resource_id
  scalable_dimension = aws_appautoscaling_target.replicas[0].scalable_dimension
  schedule           = "cron(00 19 ? * MON-FRI *)"
  timezone           = "America/New_York"

  # NOTE: min_capacity and max_capacity only impacts nodes managed by app-autoscaling
  # when this number is fewer than the number of nodes defined by the cluster
  # This will not scale-in nodes defined under aws_rds_cluster_instance.nodes
  scalable_target_action {
    min_capacity = 0
    max_capacity = 0
  }
}
