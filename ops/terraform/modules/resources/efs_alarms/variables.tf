variable "app" {}
variable "env" {}

variable "cloudwatch_notification_arn" {
  description = "The CloudWatch notification ARN."
  type        = string
}

variable "filesystem_id" {
  description = "The EFS file system ID that you want to monitor"
  type        = string
}

variable "burst_credit_balance_threshold" {
  description = "The minimum number of burst credits that a file system should have."
  type        = string
  default     = "192000000000"
  # 192 GB in Bytes (last hour where you can burst at 100 MB/sec)
}

variable "client_connections" {
  description = "The maximum number of client connections to a file system."
  type        = string
  default     = "50"
}

variable "percent_io_limit_threshold" {
  description = "IO Limit threshold"
  type        = string
  default     = "95"
}
