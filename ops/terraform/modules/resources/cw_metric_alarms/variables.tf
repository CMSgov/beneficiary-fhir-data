variable "alarm_notification_arn" {
  description = "The CloudWatch Alarm notification ARN."
  type        = "string"
  default     = null
}

variable "ok_notification_arn" {
  description = "The CloudWatch OK notification ARN."
  type        = "string"
  default     = null
}

variable "app" {
  type = string
}

variable "env" {
  type = string
}
