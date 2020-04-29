variable "database" {
  description = "Database name"
  type        = string
}

variable "bucket" {
  description = "bucket with that holds the database (Must have a databases folder)"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}

