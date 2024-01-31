variable "name" {
  description = "name"
  type        = string
}

variable "id" {
  description = "resource ID"
  type        = string
}

variable "data_set_impl_id" {
  description = "DataSet impl resource ID"
  type        = string
}

variable "data_set_prod_id" {
  description = "DataSet prod resource ID"
  type        = string
}

variable "data_set_prod_per_app_id" {
  description = "DataSet prod per_app resource ID"
  type        = string
}

variable "quicksight_groupname_owners" {
  description = "Quicksight owners group name"
  type        = string
}

variable "quicksight_groupname_admins" {
  description = "Quicksight admins group name"
  type        = string
}
