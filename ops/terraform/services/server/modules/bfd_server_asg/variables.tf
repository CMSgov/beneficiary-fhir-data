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
    is_public                  = bool
    enable_deletion_protection = bool
    server_listen_port         = string
    targets = object({
      green = object({
        ingress = object({
          port         = number
          cidrs        = list(string)
          prefix_lists = list(string)
        })
      })
      blue = object({
        ingress = object({
          port         = number
          cidrs        = list(string)
          prefix_lists = list(string)
        })
      })
    })
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

# TODO: Remove below code in BFD-3878
variable "legacy_clb_name" {
  default     = null
  type        = string
  description = "Name of the legacy CLB to associate ASGs to; only necessary for established environments"
}

variable "legacy_sg_id" {
  default     = null
  type        = string
  description = "Name of the legacy Security Group to allow ingress from in the app SG"
}
# TODO: Remove above code in BFD-3878
