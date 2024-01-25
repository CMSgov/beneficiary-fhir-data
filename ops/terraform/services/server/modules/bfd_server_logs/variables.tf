variable "env" {
  description = "The BFD SDLC environment to target"
  type        = string
  default     = "test"
}

variable "kms_key_alias" {
  description = "Key alias of environment's KMS key"
  type        = string
}
