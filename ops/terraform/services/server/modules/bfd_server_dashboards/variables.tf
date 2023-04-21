variable "dashboard_name" {
  description = "BFD dashboard name"
  type        = string
}

variable "env" {
  description = "The BFD Server SDLC environment the dashboard will represent"
  type        = string
  default     = "test"
}
