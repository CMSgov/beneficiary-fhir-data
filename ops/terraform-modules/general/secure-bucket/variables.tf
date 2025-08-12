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

variable "additional_bucket_policy_docs" {
  type        = list(string)
  default     = []
  nullable    = true
  description = "List of JSON policy document strings that are combined with the default secure Bucket policy."
}

variable "tags" {
  default     = {}
  type        = map(string)
  description = "Additional tags to attach to bucket resource."
}

variable "ssm_param_name" {
  type        = string
  nullable    = true
  default     = null
  description = <<-EOF
  Name of SSM parameter storing the name of the created bucket. Useful in contexts where the bucket
  is created using a bucket prefix instead of a static name. If null, no parameter is created.
  Defaults to null.
  EOF
}
