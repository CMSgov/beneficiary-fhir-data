variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string, zone_id = string })
}

variable "db_config" {
  description = "Sizing information for the db to create"
  type        = object({ instance_class = string, allocated_storage = number, iops = number })
}

variable "availability_zone" {
  description = "the AWS availbility zone. (eg. us-east-1a)"
  type        = string
}

variable "role" {
  description = "Examples: master, replica-az1"
  type        = string
}

variable "vpc_security_group_ids" {
  type = list(string)
}

variable "subnet_group" {
  type = string
}

variable "replicate_source_db" {
  type = string
}

variable "kms_key_id" {
  type = string
}

variable "apply_immediately" {
  type = bool
}

variable "parameter_group_name" {
  type = string
}
