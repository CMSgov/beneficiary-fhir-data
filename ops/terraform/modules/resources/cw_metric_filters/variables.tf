variable "env" {
  type        = string
}

variable "log_groups" {
  type    = object({access: string})
  default = null
}