variable "cloudwatch_notification_arn" {
  description = "The CloudWatch notification ARN."
  type        = "string"
}

variable "app" {}
variable "env" {}
variable "rds_name" {}

variable "alarm_rds_high_cpu_enable" {}
variable "alarm_rds_high_cpu_eval_periods" {}
variable "alarm_rds_high_cpu_period" {}
variable "alarm_rds_high_cpu_threshold" {}

variable "alarm_rds_free_storage_enable" {}
variable "alarm_rds_free_storage_eval_periods" {}
variable "alarm_rds_free_storage_period" {}
variable "alarm_rds_free_storage_threshold" {}

variable "alarm_rds_write_latency_enable" {}
variable "alarm_rds_write_latency_eval_periods" {}
variable "alarm_rds_write_latency_period" {}
variable "alarm_rds_write_latency_threshold" {}

variable "alarm_rds_read_latency_enable" {}
variable "alarm_rds_read_latency_eval_periods" {}
variable "alarm_rds_read_latency_period" {}
variable "alarm_rds_read_latency_threshold" {}

variable "alarm_rds_swap_usage_enable" {}
variable "alarm_rds_swap_usage_eval_periods" {}
variable "alarm_rds_swap_usage_period" {}
variable "alarm_rds_swap_usage_threshold" {}

variable "alarm_rds_disk_queue_depth_enable" {}
variable "alarm_rds_disk_queue_depth_eval_periods" {}
variable "alarm_rds_disk_queue_depth_period" {}
variable "alarm_rds_disk_queue_depth_threshold" {}

variable "alarm_rds_free_memory_enable" {}
variable "alarm_rds_free_memory_eval_periods" {}
variable "alarm_rds_free_memory_period" {}
variable "alarm_rds_free_memory_threshold" {}
