variable "s3_bucket_arns" {
  description = "A list of S3 bucket arns to allow access to"
  type        = list(string)
  default     = []
}
