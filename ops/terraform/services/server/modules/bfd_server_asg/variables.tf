variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ default_tags = map(string), vpc_id = string, azs = list(string) })
}

variable "seed_env" {
  description = "The solution's source environment. For established environments this is equal to the environment's name"
  sensitive   = false
  type        = string
}

variable "kms_key_alias" {
  description = "Key alias of environment's KMS key"
  type        = string
}

variable "role" {
  type = string
}

variable "layer" {
  description = "app or data"
  type        = string
}

variable "asg_config" {
  type = object({ min = number, max = number, max_warm = number, desired = number, sns_topic_arn = string, instance_warmup = number })
}

variable "db_config" {
  description = "Setup a db ingress rules if defined"
  type        = object({ db_sg = list(string), role = string, db_cluster_identifier = string })
  default     = { db_sg = [], role = null, db_cluster_identifier = null }
}

variable "lb_config" {
  description = "Load balancer information"
  type = object({
    name                             = string
    internal                         = optional(bool)
    load_balancer_type               = string
    enable_deletion_protection       = optional(bool)
    client_keep_alive_seconds        = optional(number)
    idle_timeout_seconds             = optional(number)
    ip_address_type                  = string
    enable_http2                     = optional(bool)
    desync_mitigation_mode           = optional(string)
    enable_cross_zone_load_balancing = optional(bool)
    access_logs = optional(object({
      access_logs_prefix = string
    }))
    connection_logs = optional(object({
      connection_logs_prefix = string
    }))
    load_balancer_listener_config = list(object({
      id                  = string
      port                = string
      protocol            = string
      default_action_type = string
    }))
    target_group_config = list(object({
      id                            = string
      name                          = string
      port                          = number
      protocol                      = string
      deregisteration_delay_seconds = number
      health_check_config = object({
        healthy_threshold             = number
        health_check_interval_seconds = number
        health_check_timeout_seconds  = number
        unhealthy_threshold           = number
      })
    }))
  })
  default = null
}

variable "mgmt_config" {
  type = object({ vpn_sg = string, tool_sg = string, remote_sg = string, ci_cidrs = list(string) })
}

variable "launch_config" {
  type = object({
    account_id        = string
    ami_id            = string
    instance_type     = string
    key_name          = string
    profile           = string
    user_data_tpl     = string
    volume_iops       = string
    volume_size       = number
    volume_throughput = number
    volume_type       = string
  })
}

variable "jdbc_suffix" {
  default     = "?logServerErrorDetail=false"
  description = "boolean controlling logging of detail SQL values if a BatchUpdateException occurs; false disables detail logging"
  type        = string
}

variable "ingress" {
  description = "Ingress port and cidr blocks"
  type        = object({ description = string, port = number, cidr_blocks = list(string), prefix_list_ids = list(string) })
}

variable "egress" {
  description = "Egress port and cidr blocks"
  type        = object({ description = string, port = number, cidr_blocks = list(string) })
}