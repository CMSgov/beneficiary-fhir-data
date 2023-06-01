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

variable "ccw_pipeline_asg_details" {
  description = "Details about the BFD CCW Pipeline ASG"
  type        = object({ arn = string, name = string })
}
