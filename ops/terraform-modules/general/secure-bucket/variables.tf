variable "bucket_name" {
  type        = string
  nullable    = true
  default     = null
  description = "Full Bucket name. Maps to `aws_s3_bucket.bucket`. Conflicts with 'bucket_prefix'."
}

variable "bucket_prefix" {
  type        = string
  nullable    = true
  default     = null
  description = "Bucket name prefix. Maps to `aws_s3_bucket.bucket_prefix`. Conflicts with 'bucket_name'."
}

variable "force_destroy" {
  type        = bool
  nullable    = false
  default     = false
  description = "Enable bucket force destroy. Maps to `aws_s3_bucket.force_destroy`."
}

variable "bucket_kms_key_arn" {
  type        = string
  nullable    = false
  description = "ARN of the KMS Key that will be used as the Bucket Key. Objects must be uploaded using this key exclusively."
}
