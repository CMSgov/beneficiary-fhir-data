variable "env" {
  description = "The BFD Server SDLC environment that the disk usage alarms Lambda is associated with"
  type        = string
  default     = "test"
}

variable "alarm_action_sns_override" {
  description = "Overrides the SNS topic that the alarms created by this module's Lambda will post to when transitioning to the ALARM state"
  type        = string
  default     = null
}

variable "alarm_ok_sns_override" {
  description = "Overrides the SNS topic that the alarms created by this module's Lambda will post to when transitioning to the OK state"
  type        = string
  default     = null
}
