variable "name" {
  description = "name for the group"
  type        = string
}

variable "policy_arns" {
  description = "List of policy arns to attach"
  type        = list(string)
}
