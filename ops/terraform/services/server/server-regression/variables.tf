variable "docker_image_tag_override" {
  description = "Overrides the Docker image URI used by the built regression suite lambda"
  type        = string
  default     = null
}
