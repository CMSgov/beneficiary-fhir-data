variable "region" {
  description = "The AWS region to target"
  type        = string
}

variable "env" {
  description = "The BFD SDLC environment to target"
  type        = string
  default     = "test"
}

variable "database_name" {
  description = "The name of the BFD Insights database that the athena views will be applied to"
  type        = string
}
