data "aws_caller_identity" "current" {}

data "aws_iam_role" "glue-role" {
  name = "bfd-insights-bfd-glue-role"
}
