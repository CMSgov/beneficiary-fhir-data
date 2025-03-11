variable "bfd_version_override" {
  default     = null
  description = "BFD release version override. When empty, defaults to resolving the release version from GitHub releases."
  type        = string
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}