variable "vpc_name" {
  description = "Name of the VPC these alarms are for."
  type        = "string"
}

variable "load_balancer_name" {
  description = "Name of the ELB these alarms are for."
  type        = "string"
}

variable "app" {}
variable "env" {}
variable "asg_name" {}
variable "rds_name" {}
variable "nat_gw_name" {}
variable "dashboard_enable" {}
