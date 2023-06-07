variable "account_id" {
  description = "BFD AWS account ID"
  type        = string
}

variable "etl_bucket_id" {
  description = "The ID of the ETL/pipeline S3 Bucket"
  type        = string
}

variable "env_kms_key_id" {
  description = "The ID of the current environment's KMS key to use"
  type        = string
}

variable "s3_events_sns_topic_name" {
  description = "The name of current environment's SNS S3 events SNS topic for use with Lambda"
  type        = string
}

variable "ccw_pipeline_asg_details" {
  description = "Details about the BFD CCW Pipeline ASG"
  type        = object({ arn = string, name = string })
}
