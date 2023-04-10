variable "ephemeral_environment_seed" {
  default     = null
  description = "The model for a novel ephemeral environment. **Required** for new ephemeral environments."
  type        = string
}

variable "ephemeral_poc" {
  default     = null
  description = "Point-of-contact name/EUA for the ephemeral environment. Defaults to bfd-{terraform.workspace}."
  type        = string
}

variable "ephemeral_rds_snapshot_id_override" {
  default     = null
  description = "Specify DB Cluster Snapshot ID from `ephemeral_environment_seed`. Defaults to latest snapshot from the seed cluster on initial definition, falls back to previously specified snapshot on subsequent execution."
  type        = string
}
