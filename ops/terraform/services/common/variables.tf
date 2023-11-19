variable "rds_cluster_identifier_override" {
  default     = null
  description = "RDS Cluster Identifier Override. Defaults to cluster identifier specified in centralized environmental configuration."
  type        = string
}

variable "rds_apply_immediately" {
  default     = false
  description = "Apply any changes to an rds cluster immediately. Use caution as this may cause downtime. Defaults to false."
  type        = bool
}
