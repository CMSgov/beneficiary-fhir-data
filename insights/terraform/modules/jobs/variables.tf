variable "project" {
  description = "Name of the project"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}

variable "buckets" {
  description = "list of buckets and cmk keys"
  type        = list(object({bucket=string, cmk=string}))
}
