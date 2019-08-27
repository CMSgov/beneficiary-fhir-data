variable "cloudwatch_notification_arn" {
  description = "The CloudWatch notification ARN."
  type        = "string"
}

variable "app" {}
variable "env" {}
variable "nat_gw_name" {}

variable "alarm_nat_error_port_alloc_enable" {}
variable "alarm_nat_error_port_alloc_eval_periods" {}
variable "alarm_nat_error_port_alloc_period" {}
variable "alarm_nat_error_port_alloc_threshold" {}

variable "alarm_nat_packets_drop_count_enable" {}
variable "alarm_nat_packets_drop_count_eval_periods" {}
variable "alarm_nat_packets_drop_count_period" {}
variable "alarm_nat_packets_drop_count_threshold" {}
