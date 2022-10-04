variable "git_repo_version" {
  description = "Branch, tag, or hash. [Details on ansible's `git` module parameter version](https://docs.ansible.com/ansible/2.9/modules/git_module.html#parameter-version)"
  default     = "master"
  type        = string
}

variable "ami_id_override" {
  description = "BFD Pipeline override ami-id. Defaults to latest pipeline/etl AMI from `master`."
  type        = string
  default     = null
}
