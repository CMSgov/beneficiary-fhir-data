variable "allocated_storage" {}
variable "storage_type" {}
variable "iops" {}
variable "instance_class" {}
variable "multi_az" {}
variable "name" {}
variable "identifier" {
  default = ""
}
variable "kms_key_id" {}
variable "db_subnet_group_name" {}
variable "snapshot_identifier" {
  default = ""
}
variable "monitoring_interval" {
  default = 15
}
variable "monitoring_role_arn" {
  default = ""
}
variable "vpc_security_group_ids" {
  type = "list"
}
variable "tags" {
  type = "map"
}
