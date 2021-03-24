variable "victor_ops_url" {
  description = "VictorOps CloudWatch integration URL"
  type        = string
}

variable "partner_acct_nums" {
  description = "Map of partner account numbers accessing EFT EFS file systems."
  type = map
}

variable "partner_subnets" {
  description = "Map of partner subnets requiring access to EFT EFS file systems"
  type        = map(map(list(string)))
}
