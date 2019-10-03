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

variable "http_500" {
  type    = object({period: number, eval_periods: number, threshold: number})
  default = null
}

variable "http_latency_4s" {
  type    = object({period: number, eval_periods: number, threshold: number, ext_stat: string})
  default = null
}

variable "http_latency_6s" {
  type    = object({period: number, eval_periods: number, threshold: number, ext_stat: string})
  default = null
}

variable "mct_query_time" {
  type    = object({period: number, eval_periods: number, threshold: number, ext_stat: string})
  default = null
}
