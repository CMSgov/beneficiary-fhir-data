variable "jenkins_ami" {
  description       = "Jenkins server AMI"
  type              = string
}


variable "vpn_security_group_id" {
  description       = "Security group that provides access via VPN"
  type              = string
}

variable "jenkins_tls_cert_arn" {
}

variable "jenkins_key_name" {
  description       = "The EC2 key pair name to assign to jenkins instances"
  type              = string
}
