variable "cloudwatch_notification_arn" {
  description = "The CloudWatch notification ARN."
  type        = "string"
}

variable "app" {}
variable "env" {}
variable "asg_name" {}

variable "alarm_status_check_failed_enable" {}
variable "alarm_status_check_failed_eval_periods" {}
variable "alarm_status_check_failed_period" {}
variable "alarm_status_check_failed_threshold" {}

variable "alarm_status_check_failed_instance_enable" {}
variable "alarm_status_check_failed_instance_eval_periods" {}
variable "alarm_status_check_failed_instance_period" {}
variable "alarm_status_check_failed_instance_threshold" {}

variable "alarm_status_check_failed_system_enable" {}
variable "alarm_status_check_failed_system_eval_periods" {}
variable "alarm_status_check_failed_system_period" {}
variable "alarm_status_check_failed_system_threshold" {}
