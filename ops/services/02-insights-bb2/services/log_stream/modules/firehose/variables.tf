variable "name" {
  type = string
}

variable "table_name" {
  type = string
}

variable "lambda_name" {
  type = string
}

variable "database" {
  type = string
}

variable "region" {
  type = string
}

variable "project" {
  type = string
}

variable "firehose_s3_buffer_size" {
  description = "Size of the buffer in MB"
  type        = number
}

variable "firehose_s3_buffer_interval" {
  description = "The interval of buffer refresh in SEC"
  type        = number
}
