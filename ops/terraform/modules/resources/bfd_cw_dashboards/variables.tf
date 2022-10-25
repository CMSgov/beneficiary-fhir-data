variable "dashboard_name" {
  description = "BFD dashboard name"
  type        = string
}

variable "asg_id" {
  description = "The name/ID of the AutoScaling Group the dashboard will show information about"
  type        = string
}
variable "env" {
  description = "The BFD Server SDLC environment the dashboard will represent"
  type        = string
  default     = "test"
}
