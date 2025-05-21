variable "ephemeral_rds_snapshot_id_override" {
  default     = null
  description = "Specify DB Cluster Snapshot ID from `seed_env`. Defaults to null which indicates latest snapshot from the seed cluster on initial definition, falls back to previously specified snapshot on subsequent execution."
  type        = string
}

variable "disable_rds_scheduling_override" {
  default     = false
  description = <<EOF
  If true, RDS off hours scheduled scale-in actions will be disabled for this environment. Defaults
  to false.
  EOF
  type        = bool
}
