variable "project" {
  description = "Name of the project"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
  default     = {}
}

variable "buckets" {
  description = "list of buckets and cmk keys"
  type        = list(object({bucket=string, cmk=string}))
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}