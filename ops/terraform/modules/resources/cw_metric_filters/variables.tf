variable "env" {
  type        = string
}

variable "create_cw_metrics" {
  description = "Whether to create the Cloudwatch log metric filter"
  type        = bool
  default     = true
}