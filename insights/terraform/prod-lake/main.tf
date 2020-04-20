locals {
  tags = {business = "OEDA", application = "bfd-insights"}
}

## S3 bucket

module "moderate_bucket" {
  source      = "../modules/bucket"
  sensitivity = "moderate"
  tags        = local.tags
}


# Provision Groups with Policies

module "group_analysts" {
  source      = "../modules/iam_group"
  name        = "analysts"
  policy_arns = [
    data.aws_iam_policy.athena_full_access.arn,
    module.kms_policies.use_moderate_cmk_arn,
    module.moderate_bucket.full_arn
  ]
}

module "group_readers" {
  source      = "../modules/iam_group"
  name        = "readers"
  policy_arns = [
  ]
}

# AWS Managed Policies 

data "aws_iam_policy" "athena_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonAthenaFullAccess"
}

data "aws_iam_policy" "quicksight_athena_access" {
  arn = "arn:aws:iam::aws:policy/AWSQuicksightAthenaAccess"
}


## Policies 

module "kms_policies" {
  source      = "../modules/kms_policies"
}



