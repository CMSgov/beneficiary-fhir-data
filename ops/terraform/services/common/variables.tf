variable "num_db_instances_override" {
  default     = null
  description = "Total number of DB instances to provision. A value of 1 will make the cluster a single-node cluster."
  type        = number

  validation {
    condition     = var.num_db_instances_override != null ? var.num_db_instances_override > 0 : true
    error_message = "Var num_db_instances_override must be a positive integer."
  }
}
