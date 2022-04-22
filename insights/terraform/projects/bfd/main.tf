locals {
  tags     = { business = "OEDA", application = "bfd-insights", project = "bfd" }
  database = "bfd"
  table    = "beneficiaries"
}

## Bucket for the project
module "bucket" {
  source      = "../../modules/bucket"
  name        = local.database
  sensitivity = "high"
  tags        = local.tags
  full_groups = [] # prevent bucket module from attempting to attach policy
}

data "aws_iam_group" "bfd_analysts" {
  group_name = "bfd-insights-analysts"
}

resource "aws_iam_group_policy" "poc" {
  name   = "foo"
  group  = data.aws_iam_group.bfd_analysts.group_name
  policy = module.bucket.iam_full_policy_body
}
