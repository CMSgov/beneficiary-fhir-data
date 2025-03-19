variable "container_insights_enabled_ephemeral_override" {
  default     = false
  description = "Override for ephemeral environments. When true, ECS Cluster `containerInsights` will be enabled. Established/Seed environments are always enabled."
  type        = bool
}
