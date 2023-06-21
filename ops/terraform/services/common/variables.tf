variable "rds_cluster_identifier_override" {
  default     = null
  description = "RDS Cluster Identifier Override. Defaults to cluster identifier specified in centralized environmental configuration."
  type        = string
}
