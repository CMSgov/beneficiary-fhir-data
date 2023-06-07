variable "account_id" {
  description = "BFD AWS account ID"
  type        = string
}

variable "etl_bucket_id" {
  description = "The ID of the ETL/pipeline S3 Bucket"
  type        = string
}

variable "aws_kms_key_arn" {
  description = "The fully qualified KMS key ARN"
  type        = string
}

variable "aws_kms_key_id" {
  description = "The ID of the KMS key to use"
  type        = string
}

variable "s3_events_sns_topic_name" {
  description = "The name of current environment's SNS S3 events SNS topic for use with Lambda"
  type        = string
}
