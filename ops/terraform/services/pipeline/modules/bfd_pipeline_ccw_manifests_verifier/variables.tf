variable "vpc_name" {
  type        = string
  description = "Name of the current VPC"
}

variable "bfd_version" {
  type        = string
  description = "BFD Lambda version to deploy"
}

variable "kms_key_alias" {
  type        = string
  description = "Alias/ID of the main CMK"
}

variable "kms_config_key_alias" {
  type        = string
  description = "Alias/ID of the config CMK"
}

variable "db_cluster_identifier" {
  type        = string
  description = "Cluster ID of the target database"
}

variable "etl_bucket_id" {
  type        = string
  description = "Bucket ID of the Pipeline ETL S3 Bucket"
}

