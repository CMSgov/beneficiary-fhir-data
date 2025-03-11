variable "docker_image_tag_override" {
  description = "Overrides the Docker image URI used by the built regression suite lambda"
  type        = string
  default     = null
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}