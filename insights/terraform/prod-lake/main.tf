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
    # Add new policies at the end
    module.moderate_bucket.full_arn,
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
  ]
}

# AWS Managed Policies 

data "aws_iam_policy" "athena_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonAthenaFullAccess"
}

data "aws_iam_policy" "quicksight_athena_access" {
  arn = "arn:aws:iam::aws:policy/AWSQuicksightAthenaAccess"
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


