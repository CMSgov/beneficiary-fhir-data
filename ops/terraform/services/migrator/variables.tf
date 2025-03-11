variable "ami_id_override" {
  default     = null
  description = "BFD Migrator override ami-id. Defaults to latest migrator AMI from `master`."
  type        = string
}

variable "create_migrator_instance" {
  default     = false
  description = "When true, create the migrator instance, security group, and RDS security group rules"
  type        = bool
}

variable "cloudtamer_iam_path" {
  type = string
  description = "IAM Pathing scheme used within Cloudtamer / KION managed AWS Accounts"
  default = "/delegatedadmin/developer/"
}