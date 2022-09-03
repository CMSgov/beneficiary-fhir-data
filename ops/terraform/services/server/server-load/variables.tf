variable "container_image_tag_node_override" {
  description = "Overrides the Container image URI used by the built load suite worker node lambda"
  type        = string
  default     = null
}

variable "create_locust_instance" {
  default     = false
  description = "When true, create the locust instance"
  type        = bool
}

variable "git_repo_version" {
  description = "Branch, tag, or hash. [Details on ansible's `git` module parameter version](https://docs.ansible.com/ansible/2.9/modules/git_module.html#parameter-version)"
  type        = string
  default     = "morgan/make-load-tests-scale-bfd-1783" #FIXME
}
