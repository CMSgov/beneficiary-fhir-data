variable "ephemeral_rds_snapshot_id_override" {
  default     = null
  description = "Specify DB Cluster Snapshot ID from `seed_env`. Defaults to null which indicates latest snapshot from the seed cluster on initial definition, falls back to previously specified snapshot on subsequent execution."
  type        = string
}
