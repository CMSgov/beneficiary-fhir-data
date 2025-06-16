variable "name" {
  description = "name"
  type        = string
}

variable "id" {
  description = "resource ID"
  type        = string
}

variable "first_app_name_select" {
  description = "First app_name for filter controls"
  type        = string
}

variable "data_set_prod_per_app_id" {
  description = "DataSet resource ID"
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
