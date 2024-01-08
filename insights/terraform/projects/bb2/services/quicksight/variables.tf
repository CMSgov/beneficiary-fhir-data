variable "datasets_global_state_map" {
  description = "global_state dataset workspace vars map"

  type = map(object({
    id                    = string
    name                  = string
    data_source_id        = string
    data_source_name      = string
    physical_table_map_id = string
  }))

  default = {
  }
}

variable "datasets_global_state_per_app_map" {
  description = "global_state_per_app dataset workspace vars map"

  type = map(object({
    id                    = string
    name                  = string
    data_source_id        = string
    data_source_name      = string
    physical_table_map_id = string
  }))

  default = {
  }
}

variable "quicksight_groupname_readers" {
  description = "Quicksight readers group name"
  type        = string
  default     = ""
}

variable "quicksight_groupname_owners" {
  description = "Quicksight owners group name"
  type        = string
  default     = ""
}

variable "quicksight_groupname_admins" {
  description = "Quicksight admins group name"
  type        = string
  default     = ""
}
