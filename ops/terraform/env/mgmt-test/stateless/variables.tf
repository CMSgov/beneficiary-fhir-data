variable "env" {
  description = "The environment to use, in this case mgmt or mgmt-test"
  type        = string
}

variable "jenkins_ami" {
  description = "Jenkins server AMI"
  type        = string
}

variable "jenkins_key_name" {
  description = "The EC2 key pair name to assign to jenkins instances"
  type        = string
}

variable "jenkins_instance_size" {
  type        = string
  description = "The EC2 instance size to use"
  default     = "c5.xlarge"
}

variable "azs" {
  description = "AZs to use"
  type        = list
}
