variable "env_config" {
  description       = "All high-level info for the whole vpc"
  type              = object({env=string, tags=map(string)})
}

variable "fhir_ami" {
  description       = "FHIR server AMI"
  type              = string
}

variable "etl_ami" {
  description       = "ETL server AMI"
  type              = string
}

variable "ssh_key_name" {
  description       = "SSH Key"
  type              = string
}
