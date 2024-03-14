variable "env_config" {
  description = "All high-level info for the whole vpc"
  type        = object({ default_tags = map(string), vpc_id = string, azs = list(string) })
}

variable "seed_env" {
  description = "The solution's source environment. For established environments this is equal to the environment's name"
  sensitive   = false
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
  type        = object({ db_sg = string, role = string, db_cluster_identifier = string })
  default     = null
}

variable "lb_config" {
  description = "Load balancer information"
  type        = object({ name = string, port = number, sg = string })
  default     = null
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

variable "scaling_networkin_interval_mb" {
  description = "The interval value in megabytes for evaluating the asg scaling capacity, based on the metric FilteredNetworkIn"
  type        = number
  default     = 100000000
}
