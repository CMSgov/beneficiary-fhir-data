variable "env" {
  description = "The Environment name passed to metric filters and dashboard"
  type        = string
}

variable "asg_id" {
  description = "Autoscaling Group ID passed to the dashboard"
  type        = string
}
