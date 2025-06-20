variable "firehose_s3_buffer_size" {
  description = "Size of the buffer in MB"
  type        = number
  default     = 5
}

variable "firehose_s3_buffer_interval" {
  description = "The interval of buffer refresh in SEC"
  type        = number
  default     = 300
}

variable "glue_crawler_schedules" {
  description = "Map of crawler schedules for envs"
  type        = map(any)
  default = {
    "test" = "cron(00 09 ? * MON *)"
    "impl" = "cron(30 08 ? * MON *)"
    "prod" = "cron(00 08 ? * MON *)"
  }
}
