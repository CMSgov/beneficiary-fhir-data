variable "kms_key_alias" {
  type        = string
  description = "Alias/ID of the main CMK"
}

variable "db_cluster_identifier" {
  type        = string
  description = "Cluster ID of the target database"
}

variable "rds_monitoring_role_arn" {
  type        = string
  description = "ARN of the RDS Enhanced Monitoring IAM Role"
}
