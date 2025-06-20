variable "datasets_global_state_prod_map" {
  description = "global_state PROD dataset workspace vars map"

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

variable "datasets_global_state_impl_map" {
  description = "global_state IMPL dataset workspace vars map"

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

variable "datasets_global_state_prod_per_app_map" {
  description = "global_state_per_app PROD dataset workspace vars map"

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

variable "analysis_prod_applications_map" {
  description = "BB2-PROD-APPLICAITONS analysis workspace vars map"

  type = map(object({
    id                    = string
    name                  = string
    first_app_name_select = string
  }))

  default = {
  }
}

variable "analysis_dasg_metrics_map" {
  description = "BB2-DASG-METRICS analysis workspace vars map"

  type = map(object({
    id   = string
    name = string
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

variable "data_set_perf_mon_id" {
  description = "DataSet perf mon ID"
  type        = string
  default     = ""
}
