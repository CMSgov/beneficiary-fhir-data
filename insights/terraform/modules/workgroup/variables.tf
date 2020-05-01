variable "name" {
  description = "Name of the workgroup"
  type        = string
}

variable "bucket" {
  description = "Bucket that will hold the /workgroups/<name>/ folder"
  type        = string
}

variable "bucket_cmk" {
  description = "The bucket's CMK"
  type        = string
}

variable "tags" {
  description = "tags"
  type        = map(string)
}
