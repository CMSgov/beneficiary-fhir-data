variable "victor_ops_url" {
  description = "VictorOps CloudWatch integration URL"
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
