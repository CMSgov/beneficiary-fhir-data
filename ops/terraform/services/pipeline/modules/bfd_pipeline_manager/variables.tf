variable "account_id" {
  description = "BFD AWS account ID"
  type        = string
}

variable "etl_bucket_id" {
  description = "The ID of the ETL/pipeline S3 Bucket"
  type        = string
}

variable "aws_env_kms_key_arn" {
  description = "The fully qualified KMS key ARN for the current environment"
  type        = string
}

variable "aws_env_kms_key_id" {
  description = "The ID of the current environment's KMS key to use"
  type        = string
}

variable "aws_mgmt_kms_key_arn" {
  description = "The fully qualified KMS key ARN for the mgmt environment"
  type        = string
}

variable "aws_mgmt_kms_key_id" {
  description = "The ID of the mgmt environment's KMS key to use"
  type        = string
}
