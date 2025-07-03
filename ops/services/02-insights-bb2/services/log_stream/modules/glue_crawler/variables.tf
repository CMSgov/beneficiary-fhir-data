variable "name" {
  type = string
}

variable "project" {
  type = string
}

variable "database" {
  type = string
}

variable "glue_crawler_schedule" {
  type = string
}

# This is to be set to the environment to use for the Insight's S3 Bucket.
# If left unset, it will use the set {local.env} S3 Bucket 
variable "insights_env" {
  type    = string
  default = ""
}
