variable "bfd_version_override" {
  default     = null
  description = "BFD release version override. When empty, defaults to resolving the release version from GitHub releases."
  type        = string
}
