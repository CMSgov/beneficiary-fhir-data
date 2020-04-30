variable "stream" {
  description = "Name of the stream"
  type        = string
}

variable "database" {
  description = "Name of the database"
  type        = string
}

variable "bucket" {
  description = "the bucket that holds the database"
  type        = string
}

variable "bucket_cmk" {
  description = "the bucket's CMK"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}

variable "buffer_size" {
  description = "Size of the buffer in MB"
  type        = number
  default     = 5
}

variable "buffer_interval" {
  description = "The interval of buffer refresh in SEC"
  type        = number
  default     = 300
}

