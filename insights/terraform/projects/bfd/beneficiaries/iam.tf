data "aws_caller_identity" "current" {}

data "aws_iam_group" "bfd-analysts" {
  group_name = "bfd-insights-analysts"
}

data "aws_iam_policy_document" "trust_rel_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["logs.us-east-1.amazonaws.com"]
    }
  }
}

data "aws_iam_role" "glue-role" {
  name = "bfd-insights-bfd-${local.environment}-iam-glue-role"
}

data "aws_iam_policy" "athena-full-access" {
  name = "AmazonAthenaFullAccess"
}

data "aws_iam_policy" "glue-service-role" {
  name = "AWSGlueServiceRole"
}
