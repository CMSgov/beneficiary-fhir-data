data "aws_caller_identity" "current" {}

data "aws_iam_group" "bfd_analysts" {
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

resource "aws_iam_group_policy" "bfd-insights-group-policy" {
  name   = "bfd-insights-${local.project}-s3-group-policy"
  group  = data.aws_iam_group.bfd_analysts.group_name
  policy = module.bucket.iam_full_policy_body
}
