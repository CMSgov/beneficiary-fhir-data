output "rds_cluster_config" {
  description = "Abbreviated JSON representation of RDS cluster for diagnostic purposes."
  value       = data.external.rds.result
}
