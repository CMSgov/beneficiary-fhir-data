data "external" "rds" {
  program = [
    "${path.module}/scripts/rds-cluster-config.sh",   # helper script
    aws_rds_cluster.aurora_cluster.cluster_identifier # verified, positional argument to script
  ]
}
