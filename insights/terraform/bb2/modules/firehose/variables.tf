variable "firehose_name" {
  type = string
}

variable "table_name" {
  type = string
}

variable "database" {
  type = string
}

variable "project" {
  type = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}

variable "buffer_size" {
  description = "Size of the buffer in MB"
  type        = number
}

variable "buffer_interval" {
  description = "The interval of buffer refresh in SEC"
  type        = number
}