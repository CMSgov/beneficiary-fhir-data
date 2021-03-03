variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ env = string, tags = map(string) })
}

variable "azs" {
  description = "AZ to use"
  type        = list
}

variable "jenkins_ami" {
  type        = string
  description = "Jenkins server AMI"
}

variable "jenkins_key_name" {
  type        = string
  description = "The EC2 key pair name to assign to jenkins instances"
}

variable "jenkins_instance_size" {
  type        = string
  description = "The EC2 instance size to use"
  default     = "c5.xlarge"
}

variable "mgmt_network_ci_cidrs" {
  type        = string
  description = "The CIDR of the MGMT Network"
  default     = "10.252.40.0/21"
}

variable "is_public" {
  description = "If true, open the FHIR data end-point to the public Internet"
  type        = bool
  default     = false
}
