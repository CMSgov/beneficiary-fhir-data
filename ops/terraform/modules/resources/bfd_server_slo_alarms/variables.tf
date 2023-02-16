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

variable "ok_sns_override" {
  description = "Overrides the default, per-environment SNS topic used for Alarms that transition from ALARM to OK"
  type        = string
  default     = null
}

variable "env" {
  description = "The BFD Server SDLC environment to deploy the CloudWatch Alarms to"
  type        = string
  default     = "test"
}
