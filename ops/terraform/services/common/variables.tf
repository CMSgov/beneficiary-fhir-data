variable "rds_apply_immediately" {
  default     = false
  description = "Apply any changes to an rds cluster immediately. Use caution as this may cause downtime. Defaults to false."
  type        = bool
}

variable "rds_deletion_protection_override" {
  default     = null
  description = <<EOF
  RDS Deletion Protection Override: `true` to enable deletion protection, `false` to disable deletion protection. The
  default `null` will use whatever the default value is for the given environment. For example, normally we disable
  deletion protection for ephemeral clusters; setting this to `true` would enable it.
  EOF
  type        = bool
}
