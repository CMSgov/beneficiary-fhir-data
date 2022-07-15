variable "table" {
  description = "Name of the table"
  type        = string
}

variable "description" {
  description = "Description of the table"
  type        = string
  default     = ""
}

variable "database" {
  description = "Name of the database that holds the table"
  type        = string
}

variable "bucket" {
  description = "the bucket that holds the database and table"
  type        = string
}

variable "bucket_cmk" {
  description = "the bucket's CMK"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}

variable "partitions" {
  description = "list of column objects"
  type        = list(object({name=string, type=string, comment=string}))
}

variable "columns" {
  description = "list of column objects"
  type        = list(object({name=string, type=string, comment=string}))
}

variable "storage_format" {
  description = "format in which data will be stored (defaults to json)"
  type        = string
  default     = "json"

  # Validate that we have an allowed value
  validation {
    condition     = contains(["json", "parquet"], var.storage_format)
    error_message = "Allowed values for storage_format are \"json\" or \"parquet\"."
  }
}

variable "serde_format" {
  description = "format for serialization / deserialization (defaults to json)"
  type        = string
  default     = "json"

  # Validate that we have an allowed value
  validation {
    condition     = contains(["json", "grok", "parquet"], var.serde_format)
    error_message = "Allowed values for serde_format are \"json\", \"grok\", or \"parquet\"."
  }
}

variable "serde_parameters" {
  description = "parameters for serde (optional)"
  type        = map
  default     = {}
}
