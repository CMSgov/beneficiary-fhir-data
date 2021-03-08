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

variable "bcda_acct_num" {
  description = "BCDA AWS account number accessing EFT EFS file systems"
  type        = string
}

variable "bcda_subnets" {
  description = "BCDA subnets requiring access to EFT EFS file systems"
  type        = map(list(string))
}
