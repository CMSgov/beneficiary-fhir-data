variable "docker_image_uri_override" {
  description = "Overrides the Docker image URI used by the built regression suite lambda"
  type        = string
} 

variable "regression_suite_api_version" {
  default     = "v2"
  description = "API version of the Locust regression suite to run in Lambda"
  type        = string
}

variable "regression_suite_spawn_rate" {
  default     = 5
  description = "Locust user spawn rate for regression suite tests"
  type        = number
}

variable "regression_suite_num_users" {
  default     = 5
  description = "Number of total users to spawn for regression suite tests"
  type        = number
}

variable "regression_suite_spawned_runtime" {
  default     = "1m"
  description = "Amount of time tests should continue to run once all users have spawned"
  type        = string
}