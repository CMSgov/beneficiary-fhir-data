variable "retain" {
  description = "Number of days to retain the snapshots"
  type        = number
}

<<<<<<< HEAD
variable "time" {
  description = "Snapshot start time"
  type        = string
  default     = "23:45" 
}
=======
variable "interval" {
  description = "Snapshot interval in hours"
  type        = number
}
>>>>>>> Add DLM schedule
