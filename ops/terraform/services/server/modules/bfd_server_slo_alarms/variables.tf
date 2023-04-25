variable "alert_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Alert SLO alarms"
  type        = string
  default     = null
}

variable "warning_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Warning SLO alarms"
  type        = string
  default     = null
}

variable "alert_ok_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Alert Alarms that transition from ALARM to OK"
  type        = string
  default     = null
}

variable "warning_ok_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Warning Alarms that transition from ALARM to OK"
  type        = string
  default     = null
}
