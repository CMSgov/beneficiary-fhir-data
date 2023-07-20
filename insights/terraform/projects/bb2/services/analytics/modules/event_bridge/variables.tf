variable "name" {
  description = "Event bridge rule name"
  type        = string
}

variable "description" {
  description = "Event bridge rule description text"
  type        = string
}

variable "schedule" {
  description = "Event bridge rule CRON type schedule string"
  type        = string
}
