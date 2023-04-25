variable "alert_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for high-severity Alarm notifications"
  type        = string
  default     = null
}

variable "notify_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for low-severity Alarm notifications"
  type        = string
  default     = null
}

variable "ok_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Alarms that transition from ALARM to OK"
  type        = string
  default     = null
}
