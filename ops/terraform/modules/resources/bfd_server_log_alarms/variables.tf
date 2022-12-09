variable "alarm_notification_arn" {
  description = "The ARN for an SNS Topic to send notifications about alarms transitioning to ALARM"
  type        = string
  default     = null
}

variable "ok_notification_arn" {
  description = "The ARN for an SNS Topic to send notifications about alarms that have recovered"
  type        = string
  default     = null
}

variable "env" {
  description = "The BFD Server SDLC environment to deploy the CloudWatch Alarms to"
  type        = string
  default     = "test"
}
