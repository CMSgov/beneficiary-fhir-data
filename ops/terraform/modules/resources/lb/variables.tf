variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({env=string, tags=map(string), vpc_id=string, zone_id=string, azs=list(string)})
}

variable "role" {
  description = "The role of this lb in the application. Will be used to "
  type        = string
}

variable "layer" {
  description = "app or data"
  type        = string 
}

variable "log_bucket" {
  type        = string
}

variable "is_public" {
  description = "If true, the LB is created as a public LB"
  type        = bool
  default     = false
}

variable "listeners" {
  description = "The listeners that the load balancer should have."
  type        = list(
                  object({
                    ingress_description = string,
                    ingress_port        = number,
                    ingress_cidr_blocks = list(string),
                    egress_description  = string,
                    egress_port         = number,
                    egress_cidr_blocks  = list(string),
                  })
                )
}

variable "health_check_port" {
  description = "Configures the load balancer's health check."
  type        = number
}
