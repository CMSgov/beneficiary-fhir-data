data "aws_iam_policy" "rds_monitoring" {
  name = "AmazonRDSEnhancedMonitoringRole"
}

data "aws_iam_policy_document" "rds_monitoring_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["monitoring.rds.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [local.account_id]
    }
  }
}

# Role allowing monitoring for RDS Clusters and instances
resource "aws_iam_role" "db_monitoring" {
  name                 = "bfd-fhirdb-${local.env}-rds-monitoring"
  path                 = local.iam_path
  permissions_boundary = local.permissions_boundary_arn
  assume_role_policy   = data.aws_iam_policy_document.rds_monitoring_assume.json
}

resource "aws_iam_role_policy_attachment" "db_monitoring" {
  role       = aws_iam_role.db_monitoring.name
  policy_arn = data.aws_iam_policy.rds_monitoring.arn
}
