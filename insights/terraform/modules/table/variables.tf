variable "table" {
  description = "Name of the table"
  type        = string
}

variable "database" {
  description = "Name of the project"
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

variable "partitions" {
  description = "list of column objects"
  type        = list(object({name=string, type=string, comment=string}))
}

variable "columns" {
  description = "list of column objects"
  type        = list(object({name=string, type=string, comment=string}))
}




