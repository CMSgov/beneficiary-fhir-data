variable "asg_names" {
  description = "Names of the ASG to attach notifications to"
  type        = list(string)
  default     = null
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

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}