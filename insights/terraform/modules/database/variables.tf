variable "database" {
  description = "Database name"
  type        = string
}

variable "sensitivity" {
  description = "Sensitivity name (ie. high or moderate)"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}


