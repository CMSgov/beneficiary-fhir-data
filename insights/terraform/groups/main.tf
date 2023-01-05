# Add to the IAM groups

data "aws_iam_group" "groups" {
  for_each    = var.insights_group_members
  group_name  = "bfd-insights-${each.key}"
}

resource "aws_iam_group_membership" "team" {
  for_each    = var.insights_group_members
  name        = "bfd-insights-${each.key}-team"
  group       = data.aws_iam_group.groups[each.key].group_name
  users       = each.value
}


# Create ad-hoc folders for analysts

data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "main" {
  bucket        = "bfd-insights-moderate-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_object" "user_folder" {
  for_each      = toset(var.insights_group_members["analysts"])
  bucket        = data.aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "users/${each.value}/"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_object" "user_output_folder" {
  depends_on    = [aws_s3_bucket_object.user_folder]
  for_each      = toset(var.insights_group_members["analysts"])
  bucket        = data.aws_s3_bucket.main.id
  content_type  = "application/x-directory"
  key           = "users/${each.value}/query_results"

  lifecycle {
    prevent_destroy = true
  }
}

locals {
  account_id                       = data.aws_caller_identity.current.account_id
  azs                              = ["us-east-1a", "us-east-1b", "us-east-1c"]
  env                              = var.env
  env_config                       = { env = local.env, vpc_id = data.aws_vpc.main.id, zone_id = module.local_zone.zone_id }
  is_prod                          = substr(var.env, 0, 4) == "prod"
  victor_ops_url                   = var.victor_ops_url
  enable_victor_ops                = local.is_prod # only wake people up for prod alarms
  cloudwatch_sns_topic_policy_spec = <<-EOF
{
  "Version": "2008-10-17",
  "Id": "__default_policy_ID",
  "Statement": [
    {
        "Sid": "Allow_Publish_Alarms",
        "Effect": "Allow",
        "Principal":
        {
            "Service": [
                "cloudwatch.amazonaws.com"
            ]
        },
        "Action": "sns:Publish",
        "Resource": "%s"
    },
    {
      "Sid": "__default_statement_ID",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "SNS:GetTopicAttributes",
        "SNS:SetTopicAttributes",
        "SNS:AddPermission",
        "SNS:RemovePermission",
        "SNS:DeleteTopic",
        "SNS:Subscribe",
        "SNS:ListSubscriptionsByTopic",
        "SNS:Publish",
        "SNS:Receive"
      ],
      "Resource": "%s",
      "Condition": {
        "StringEquals": {
          "AWS:SourceOwner": "${local.account_id}"
        }
      }
    }
  ]
}
EOF
}





