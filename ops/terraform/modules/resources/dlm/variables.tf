variable "retain" {
  description = "Number of days to retain the snapshots"
  type        = number
}

variable "time" {
  description = "Snapshot start time"
  type        = string
  default     = "23:45"
}
