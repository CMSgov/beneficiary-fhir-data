variable "ssm_config" {
  type        = map(string)
  description = "SSM config retrieved by parent Terraservice"
}

variable "kms_key_arn" {
  type        = string
  description = "The current environment's default KMS key ARN"
}

variable "outbound_lambda_name" {
  type        = string
  description = "Name of BFD EFT SFTP Outbound Lambda"
}

variable "outbound_lambda_dlq_name" {
  type        = string
  description = "Name of the BFD EFT SFTP Outbound Lambda's DLQ"
}

variable "outbound_bfd_sns_topic_name" {
  type        = string
  description = "The name of the catch-all/BFD BFD EFT Outbound SNS Topic"
}

variable "outbound_bfd_sns_topic_arn" {
  type        = string
  description = "The ARN of the catch-all/BFD BFD EFT Outbound SNS Topic"
}

variable "outbound_partner_sns_topic_names" {
  type        = list(string)
  description = "List of names of the partner-specific BFD EFT Outbound SNS Topics"
}
