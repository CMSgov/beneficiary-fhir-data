variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string), vpc_id = string })
}

variable "name" {
  description = "Domain name for the zone. Only needed for public domains"
  type        = string
  default     = null
}

variable "parent" {
  description = "Parent zone. Create a NAPTR record to this zone in the parent zone"
  type        = object({ name = string, zone_id = string })
  default     = null
}

variable "public" {
  description = "If true, declare the zone as a public zone"
  type        = bool
  default     = false
}

variable "apex_record" {
  description = "Create an APEX record for an AWS resource"
  type        = object({ alias = string, zone_id = string })
  default     = null
}

variable "a_records" {
  description = "A list of A records that AWS resources"
  type        = list(object({ name = string, alias = string, zone_id = string }))
  default     = []
}
