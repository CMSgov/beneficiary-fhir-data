variable "env" {
  type = string
}

variable "metric_config" {
  type = object({
    partner_name  = string,
    partner_regex = string
  })
}
