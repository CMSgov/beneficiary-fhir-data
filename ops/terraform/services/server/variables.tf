variable "ami_id_override" {
  description = "BFD Server override ami-id. Defaults to latest server/fhir AMI from `git_branch_name|master`."
  type        = string
  default     = null
}

variable "jdbc_suffix" {
  default     = "?logServerErrorDetail=false"
  description = "boolean controlling logging of detail SQL values if a BatchUpdateException occurs; false disables detail logging"
  type        = string
}

variable "force_create_server_lb_alarms" {
  description = "Forces the creation of bfd_server_lb_alarms for ephemeral environments"
  default     = false
  type        = bool
}

variable "force_create_server_metrics" {
  description = "Forces the creation of bfd_server_metrics for ephemeral environments"
  default     = false
  type        = bool
}

variable "force_create_server_slo_alarms" {
  description = "Forces the creation of bfd_server_slo_alarms for ephemeral environments"
  default     = false
  type        = bool
}

variable "force_create_server_log_alarms" {
  description = "Forces the creation of bfd_server_log_alarms for ephemeral environments"
  default     = false
  type        = bool
}

variable "force_create_server_dashboards" {
  description = "Forces the creation of bfd_server_dashboards for ephemeral environments"
  default     = false
  type        = bool
}

variable "force_create_server_disk_alarms" {
  description = "Forces the creation of bfd_server_disk_alarms for ephemeral environments"
  default     = false
  type        = bool
}

variable "force_create_server_error_alerts" {
  description = "Forces the creation of bfd_server_error_alerts for ephemeral environments"
  default     = false
  type        = bool
}

variable "git_branch_name" {
  default     = "master"
  description = "The name of the git branch to reference during creation. Defaults to `master`"
  type        = string
}
