variable "account_id" {
  description = "BFD AWS account ID"
  type        = string
}

variable "insights_bucket_arn" {
  description = "The ARN of the BFD Server's Insights S3 Bucket"
  type        = string
}

variable "name_prefix" {
  description = "Global name prefix that is used to prefix all BFD Server Insights resources"
  type        = string
}

variable "glue_database" {
  description = "Name of the Glue Database that the Lambda this module configures will target"
  type        = string
}

variable "glue_table" {
  description = "Name of the Glue Table that the Lambda this module configures will target"
  type        = string
}

variable "glue_crawler_name" {
  description = "The name of the BFD Server API requests Glue Crawler"
  type        = string
}

variable "glue_crawler_arn" {
  description = "The ARN of the BFD Server API requests Glue Crawler"
  type        = string
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}