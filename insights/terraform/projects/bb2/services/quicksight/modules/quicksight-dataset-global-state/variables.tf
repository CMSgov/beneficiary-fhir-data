variable "id" {
  description = "DataSet resource ID"
  type        = string
}

variable "name" {
  description = "DataSet name"
  type        = string
}

variable "data_source_id" {
  description = "DataSource resource ID"
  type        = string
}

variable "data_source_name" {
  description = "DataSource name"
  type        = string
}

variable "physical_table_map_id" {
  description = "DataSource physical table map id"
  type        = string
}

variable "quicksight_groupname_readers" {
  description = "Quicksight readers group name"
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
