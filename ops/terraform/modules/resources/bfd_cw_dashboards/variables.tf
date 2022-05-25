variable "dashboard_name" {
  description = "BFD dashboard name"
  type        = string
}

variable "dashboard_namespace" {
  description = "BFD dashboard namespace"
  type        = string
}

variable "asg" {
  description = "The name of AWS autoscaling group"
  type        = string
}
