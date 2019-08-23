variable "env_config" {
  description       = "All high-level info for the whole vpc"
  type              = object({env=string, tags=map(string)})
}

variable "jenkins_ami" {
  description       = "Jenkins server AMI"
  type              = string
}

variable "vpn_security_group_id" {
  type              = string
  description       = "Security group that provides access via VPN"
}

variable "jenkins_tls_cert_arn" {
}

variable "jenkins_key_name" {
  type              = string
  description       = "The EC2 key pair name to assign to jenkins instances"
}

variable "instance_size" {
  type              = "string"
  description       = "The EC2 instance size to use"
  default           = "m5.xlarge"
}
