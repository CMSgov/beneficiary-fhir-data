variable "lambda_name" {
  description = "The name of the trigger Lambda that will be created by this module"
  nullable    = false
  type        = string
}

variable "iam_path" {
  description = "The IAM path to apply to all IAM Policies created by this module"
  type        = string
}

variable "iam_permissions_boundary_arn" {
  description = "The Permissions Boundary ARN to apply as the Permissions Boundary for all IAM Roles created by this module"
  type        = string
}

variable "kms_key_arn" {
  description = "ARN of the KMS key used to encrypt all resources"
  type        = string
}

variable "crawler_name" {
  description = "Name of the Glue Crawler that the trigger Lambda will invoke"
  nullable    = false
  type        = string
}

variable "crawler_arn" {
  description = "ARN of the Glue Crawler that the trigger Lambda will invoke"
  nullable    = false
  type        = string
}

variable "database_name" {
  description = "Name of the Glue Database that will be crawled"
  nullable    = false
  type        = string
}

variable "table_name" {
  description = "Name of the Glue Table that will be crawled"
  nullable    = false
  type        = string
}

variable "partitions" {
  description = "List of Table partitions. If there are any new partition values for any of these partitions, the trigger Lambda will start the Glue Crawler"
  nullable    = false
  type        = list(string)
}
