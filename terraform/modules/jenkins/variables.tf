variable "vpc_id" {}

variable "tls_cert_arn" {}

variable "app_subnets" {
  type        = "list"
  description = "Subnets to use for the jenkins application"
}

variable "elb_subnets" {
  type        = "list"
  description = "Subnets to use for the jenkins elb"
}

variable "vpn_security_group_id" {
  type        = "string"
  description = "Security group that provides access via VPN"
}

variable "ami_id" {
  type        = "string"
  description = "Jenkins base AMI ID to use."
}

variable "key_name" {
  type        = "string"
  description = "The EC2 key pair name to assign to jenkins instances"
}

variable "instance_type" {
  type        = "string"
  description = "The EC2 instance size to use"
  default     = "m5.xlarge"
}
