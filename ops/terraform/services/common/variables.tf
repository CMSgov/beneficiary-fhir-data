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

variable "disable_rds_scheduling_override" {
  default     = false
  description = <<EOF
  If true, RDS off hours scheduled scale-in actions will be disabled for this environment. Defaults
  to false.
  EOF
  type        = bool
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}