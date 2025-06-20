locals {
  tags = {business = "OEDA", application = "bfd-insights"}
}

## S3 bucket

module "moderate_bucket" {
  source      = "../modules/bucket"
  name        = "moderate"
  full_groups = [module.group_analysts.name]
  athena_groups = [module.group_readers.name, module.group_authors.name]
  folders     = ["databases", "adhoc", "users", "workgroups"]
  tags        = local.tags
}


# Provision Groups with Policies

module "group_analysts" {
  source      = "../modules/iam_group"
  name        = "analysts"
  policy_arns = [
    # Add new policies at the end
    data.aws_iam_policy.athena_full_access.arn,
    data.aws_iam_policy.kinesis_firehose_full_access.arn,
    data.aws_iam_policy.kinesis_stream_full_access.arn,
    data.aws_iam_policy.kinesis_analytics_full_access.arn,
    data.aws_iam_policy.glue_full_access.arn
  ]
}

module "group_readers" {
  source      = "../modules/iam_group"
  name        = "readers"
  policy_arns = [
    # Add new policies at the end
    data.aws_iam_policy.quicksight_athena_access.arn
  ]
}

module "group_authors" {
  source      = "../modules/iam_group"
  name        = "authors"
  policy_arns = [
    # Add new policies at the end
    data.aws_iam_policy.quicksight_athena_access.arn
  ]
}

# AWS Managed Policies 

data "aws_iam_policy" "athena_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonAthenaFullAccess"
}

data "aws_iam_policy" "quicksight_athena_access" {
  arn = "arn:aws:iam::aws:policy/service-role/AWSQuicksightAthenaAccess"
}

data "aws_iam_policy" "kinesis_firehose_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonKinesisFirehoseFullAccess"
}

data "aws_iam_policy" "kinesis_stream_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonKinesisFullAccess"
}

data "aws_iam_policy" "kinesis_analytics_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonKinesisAnalyticsFullAccess"
}

data "aws_iam_policy" "glue_full_access" {
  arn = "arn:aws:iam::aws:policy/AWSGlueConsoleFullAccess"
}

## Policies 


