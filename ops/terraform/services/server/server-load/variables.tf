variable "docker_image_tag_broker_override" {
  description = "Overrides the Docker image URI used by the built load suite broker lambda"
  type        = string
  default     = null
}

variable "docker_image_tag_controller_override" {
  description = "Overrides the Docker image URI used by the built load suite controller lambda"
  type        = string
  default     = null
}

variable "docker_image_tag_worker_override" {
  description = "Overrides the Docker image URI used by the built load suite worker lambda"
  type        = string
  default     = null
}
