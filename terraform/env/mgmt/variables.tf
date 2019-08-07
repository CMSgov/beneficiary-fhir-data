variable "env" {
}

variable "vpn_security_group_id" {
  type        = string
  description = "Security group that provides access via VPN"
}

variable "jenkins_tls_cert_arn" {
}

variable "jenkins_ami_id" {
  type        = string
  description = "Jenkins AMI ID to use."
}

variable "jenkins_key_name" {
  type        = string
  description = "The EC2 key pair name to assign to jenkins instances"
}
