variable "name" {
  description = "Lambda function name"
  type        = string
}

variable "description" {
  description = "Lambda function description text"
  type        = string
}

variable "role" {
  description = "Lambda function role"
  type        = string
}

variable "region" {
  description = "Lambda function AWS region"
  type        = string
}

variable "application" {
  description = "Lambda function application tag"
  type        = string
}

variable "project" {
  description = "Lambda function project tag"
  type        = string
}
