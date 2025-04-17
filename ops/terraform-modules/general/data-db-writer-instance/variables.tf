variable "cluster_identifier" {
  type        = string
  nullable    = false
  description = "RDS Cluster Identifier. Maps to data.aws_rds_cluster.cluster_identifier."
}
