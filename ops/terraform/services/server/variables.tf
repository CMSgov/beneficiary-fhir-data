variable "ami_id_override" {
  description = "BFD Server override ami-id. Defaults to latest server/fhir AMI from `master`."
  type        = string
  default     = null
}

variable "jdbc_suffix" {
  default     = "?logServerErrorDetail=false"
  description = "Suffix added to the Database JDBC URL to set various JDBC parameters"
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

variable "db_environment_override" {
  default     = null
  description = "For use in database maintenance contexts only."
  sensitive   = false
  type        = string
}

# BFD-2588
variable "disable_asg_autoscale_alarms" {
  default     = false
  description = "For use in ASG management in release pipeline contexts only."
  sensitive   = false
  type        = bool
}

variable "override_asg_desired_instance_factor" {
  default     = 1
  description = "For use by release pipeline contexts only."
  sensitive   = false
  type        = number
}