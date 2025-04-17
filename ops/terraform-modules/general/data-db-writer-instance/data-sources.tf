# Data lookup is technically unnecessary, but is here in order to fail-fast prior to passing cluster
# identifier to external data source
data "aws_rds_cluster" "main" {
  cluster_identifier = var.cluster_identifier
}

data "external" "writer_identifier" {
  program = [
    "${path.module}/scripts/rds-writer-identifier.sh", # helper script
    data.aws_rds_cluster.main.cluster_identifier       # verified, positional argument to script
  ]
}

data "aws_db_instance" "writer" {
  db_instance_identifier = data.external.writer_identifier.result.writer
}
