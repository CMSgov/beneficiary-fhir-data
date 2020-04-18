variable "stream" {
  description = "name of the stream"
  type        = string
}

variable "sensitivity" {
  description = "Sensitivity name (ie. high or moderate)"
  type        = string
}

variable "bucket_arn" {
  description = "ARN for the delivery bucket"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}

