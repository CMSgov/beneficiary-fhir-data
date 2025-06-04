variable "ephemeral_rds_snapshot_id_override" {
  default     = null
  description = <<-EOF
  Specify DB Cluster Snapshot ID from `seed_env`. Will cause the Cluster to be created from a
  Snapshot. Defaults to null which, for ephemeral environments, indicates latest snapshot from the
  seed cluster on initial definition, falling back to previously specified snapshot on subsequent
  execution.
  EOF
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
