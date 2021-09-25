variable "victor_ops_url" {
  description = "VictorOps CloudWatch integration URL"
  type        = string
}

variable "partner_acct_nums" {
  description = "Map of partner account numbers accessing EFT EFS file systems."
  type        = map
}

variable "partner_subnets" {
  description = "Map of partner subnets requiring access to EFT EFS file systems"
  type        = map(map(list(string)))
}

variable "medicare_opt_out_config" {
  description = "Config for medicare opt out S3 bucket"
  type        = object({ read_roles = list(string), write_accts = list(string), admin_users = list(string) })
}

variable "connect_direct_reserved_ip" {
  description = "IP address to assign to the connect direct service."
  type        = string
}
