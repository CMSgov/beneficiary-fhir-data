
variable "insights_group_members" {
  description = "Config for medicare opt out S3 bucket"
  type        = object({ analysts = list(string), authors = list(string), readers = list(string) })
}
