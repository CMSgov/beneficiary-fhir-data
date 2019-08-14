variable "vpc_name" {
  description = "Name of the VPC these alarms are for."
  type        = "string"
}

variable "load_balancer_name" {
  description = "Name of the ELB these alarms are for."
  type        = "string"
}

variable "cloudwatch_notification_arn" {
  description = "The CloudWatch notification ARN."
  type        = "string"
}

variable "app" {}
variable "env" {}

variable "alarm_elb_no_backend_enable" {}
variable "alarm_elb_no_backend_eval_periods" {}
variable "alarm_elb_no_backend_period" {}
variable "alarm_elb_no_backend_threshold" {}

variable "alarm_elb_high_latency_enable" {}
variable "alarm_elb_high_latency_eval_periods" {}
variable "alarm_elb_high_latency_period" {}
variable "alarm_elb_high_latency_threshold" {}

variable "alarm_elb_spillover_count_enable" {}
variable "alarm_elb_spillover_count_eval_periods" {}
variable "alarm_elb_spillover_count_period" {}
variable "alarm_elb_spillover_count_threshold" {}

variable "alarm_elb_surge_queue_length_enable" {}
variable "alarm_elb_surge_queue_length_eval_periods" {}
variable "alarm_elb_surge_queue_length_period" {}
variable "alarm_elb_surge_queue_length_threshold" {}

variable "alarm_backend_4xx_enable" {}
variable "alarm_backend_4xx_eval_periods" {}
variable "alarm_backend_4xx_period" {}
variable "alarm_backend_4xx_threshold" {}

variable "alarm_backend_5xx_enable" {}
variable "alarm_backend_5xx_eval_periods" {}
variable "alarm_backend_5xx_period" {}
variable "alarm_backend_5xx_threshold" {}

variable "alarm_elb_5xx_enable" {}
variable "alarm_elb_5xx_eval_periods" {}
variable "alarm_elb_5xx_period" {}
variable "alarm_elb_5xx_threshold" {}
