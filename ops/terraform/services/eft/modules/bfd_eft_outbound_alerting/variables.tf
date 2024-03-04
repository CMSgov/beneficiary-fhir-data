variable "ssm_config" {
  type        = map(string)
  description = "SSM config retrieved by parent Terraservice"
}

variable "outbound_lambda_name" {
  type        = string
  description = "Name of BFD EFT SFTP Outbound Lambda"
  default     = null
}

variable "outbound_lambda_dlq_name" {
  type        = string
  description = "Name of the BFD EFT SFTP Outbound Lambda's DLQ"
  default     = null
}

variable "outbound_sns_topic_names" {
  type        = list(string)
  description = "List of names of the BFD EFT Outbound SNS Topics"
  default     = []
}
