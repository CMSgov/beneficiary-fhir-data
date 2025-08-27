variable "iam_path" {
  type        = string
  nullable    = false
  description = "Kion-compliant IAM path"
}

variable "permissions_boundary_arn" {
  type        = string
  nullable    = false
  description = "Kion-compliant Permissions Boundary ARN"
}

variable "tags" {
  default     = {}
  type        = map(string)
  description = "Additional tags to attach to resources."
}

variable "topic_name" {
  type        = string
  nullable    = false
  description = "SNS Topic name. Maps to `aws_sns_topic.name`."
}

variable "topic_description" {
  type        = string
  nullable    = true
  default     = null
  description = "SNS Topic description. Maps to `aws_sns_topic.display_name`."
}

variable "additional_topic_policy_docs" {
  type        = list(string)
  default     = []
  nullable    = true
  description = "List of JSON policy document strings that are combined with the default SNS Topic policy. Use the placeholder string \"%TOPIC_ARN%\" in policies to represent the topic ARN."
}

variable "kms_key_arn" {
  type        = string
  nullable    = false
  description = "ARN of the KMS Key that will be used to encrypt the topic and the logs it emits."
}

variable "sqs_sample_rate" {
  type        = number
  nullable    = false
  default     = 0
  description = "Sample rate, in percent, of SQS events to log. Defaults to 0%"
}

variable "application_sample_rate" {
  type        = number
  nullable    = false
  default     = 0
  description = "Sample rate, in percent, of application events to log. Defaults to 0%"
}

variable "http_sample_rate" {
  type        = number
  nullable    = false
  default     = 0
  description = "Sample rate, in percent, of HTTP events to log. Defaults to 0%"
}

variable "lambda_sample_rate" {
  type        = number
  nullable    = false
  default     = 0
  description = "Sample rate, in percent, of Lambda events to log. Defaults to 0%"
}

variable "firehose_sample_rate" {
  type        = number
  nullable    = false
  default     = 0
  description = "Sample rate, in percent, of Firehose events to log. Defaults to 0%"
}
