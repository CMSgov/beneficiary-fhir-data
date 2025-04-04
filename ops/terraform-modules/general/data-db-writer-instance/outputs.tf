output "writer" {
  value       = data.aws_db_instance.writer
  description = "`data.aws_db_instance` data resource corresponding to the WRITER Instance of the provided Cluster."
}
