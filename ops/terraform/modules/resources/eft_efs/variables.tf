variable "partner" {
  description = "Parnert name. E.g., 'bcda', 'dpc', etc."
  type        = string
}

variable "partner_acct_num" {
  description = "Partners AWS Account Number"
  type        = string
}

variable "partner_subnets" {
  description = "Partner subnets requiring access to EFT EFS file systems"
  type        = map(list(string))
}

variable "posix_uid" {
  description = "High numbered posix id for tracking partner owned files. E.g., 1500, 1501, etc"
}

variable "posix_gid" {
  description = "High numbered posix id for tracking partner owned files. E.g., 1500, 1501, etc"
}

# NOTE: the path must be prefixed with / but no trailing /
variable "partner_root_dir" {
  description = "Name of the folder within the EFS fs that we want to root our partners into."
  default     = "/dropbox"
}

variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string })
}

variable "role" {
  type = string
}

variable "layer" {
  description = "app or data"
  type        = string
}