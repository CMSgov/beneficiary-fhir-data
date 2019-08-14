variable "name" {}
variable "env" {}
variable "ssl_certificate_id" {}
variable "access_log_interval" {
  default = 60
}
variable "idle_timeout" {
  default = 60
}
