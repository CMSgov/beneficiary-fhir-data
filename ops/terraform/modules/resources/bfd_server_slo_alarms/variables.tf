variable "alert_notification_arn" {
  description = "The ARN for an SNS Topic to send notifications about SLOs exceeding ALERT thresholds to"
  type        = string
  default     = null
}

variable "warning_notification_arn" {
  description = "The ARN for an SNS Topic to send notifications about SLOs exceeding WARNING thresholds to"
  type        = string
  default     = null
}

variable "ok_notification_arn" {
  description = "The ARN for an SNS Topic to send notifications about SLOs that have recovered"
  type        = string
  default     = null
}

variable "env" {
  description = "The BFD Server SDLC environment to deploy the CloudWatch Alarms to"
  type        = string
  default     = "test"
}
