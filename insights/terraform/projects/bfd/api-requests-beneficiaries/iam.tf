data "aws_caller_identity" "current" {}

data "aws_iam_role" "iam-role-glue" {
  name = "bfd-insights-bfd-glue-role"
}
