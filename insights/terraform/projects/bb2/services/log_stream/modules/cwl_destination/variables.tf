variable "firehose_name" {
  type = string
}

variable "project" {
  type = string
}

variable "region" {
  type = string
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}