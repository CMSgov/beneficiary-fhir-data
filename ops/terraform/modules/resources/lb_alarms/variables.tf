variable "load_balancer_name" {
  description = "Name of the ELB these alarms are for."
  type        = string
}

variable "alarm_notification_arn" {
  description = "The CloudWatch Alarm notification ARN."
  type        = string
  default     = null
}

variable "ok_notification_arn" {
  description = "The CloudWatch OK notification ARN."
  type        = string
  default     = null
}

variable "app" {
  type = string
}

variable "env" {
  type = string
}


# Common Metrics 
#

variable "healthy_hosts" {
  type    = object({ period : number, eval_periods : number, threshold : number })
  default = null
}


## Classic ELB Metrics
#
variable "clb_spillover_count" {
  type    = object({ period : number, eval_periods : number, threshold : number })
  default = null
}

variable "clb_surge_queue_length" {
  type    = object({ period : number, eval_periods : number, threshold : number })
  default = null
}


## ALB Metrics
#

variable "alb_high_latency" {
  type    = object({ period : number, eval_periods : number, threshold : number })
  default = null
}

variable "alb_status_4xx" {
  type    = object({ period : number, eval_periods : number, threshold : number })
  default = null
}

variable "alb_rate_of_5xx" {
  type    = object({ period : number, eval_periods : number, threshold : number })
  default = null
}
