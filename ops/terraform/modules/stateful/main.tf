## 
# Stateful resources for an environment and associated KMS needed by both stateful and stateless resources

locals {
  account_id       = data.aws_caller_identity.current.account_id
  azs              = ["us-east-1a", "us-east-1b", "us-east-1c"]
  established_envs = ["test", "prod-sbx", "prod"]
  env              = var.env
  env_config       = { env = local.env, vpc_id = data.aws_vpc.main.id, zone_id = module.local_zone.zone_id }
  is_prod          = substr(var.env, 0, 4) == "prod"
  is_ephemeral_env = !(contains(local.established_envs, var.env))

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

data "aws_caller_identity" "current" {}

data "aws_s3_bucket" "logs" {
  bucket        = "bfd-${local.env}-logs-${local.account_id}"
  force_destroy = local.is_ephemeral_env
}

data "aws_s3_bucket" "etl" {
  bucket        = "bfd-${local.env}-etl-${local.account_id}"
  force_destroy = local.is_ephemeral_env
}

# vpc
data "aws_vpc" "main" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpc"]
  }
}

# kms 
data "aws_kms_key" "master_key" {
  key_id = "alias/bfd-${local.env}-cmk"
}

# subnets
data "aws_subnet" "data_subnets" {
  count             = length(local.azs)
  vpc_id            = data.aws_vpc.main.id
  availability_zone = local.azs[count.index]
  filter {
    name   = "tag:Layer"
    values = ["data"]
  }
}

# vpn
data "aws_security_group" "vpn" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-vpn-private"]
  }
}

# management security group
data "aws_security_group" "tools" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-enterprise-tools"]
  }
}

# tools security group 
data "aws_security_group" "management" {
  filter {
    name   = "tag:Name"
    values = ["bfd-${local.env}-remote-management"]
  }
}


## VPC Private Local Zone for CNAME Records
#
module "local_zone" {
  source     = "../resources/dns"
  env_config = { env = var.env, vpc_id = data.aws_vpc.main.id }
  public     = false
}


## CloudWatch SNS Topics for Alarms
#
resource "aws_sns_topic" "cloudwatch_alarms" {
  name              = "bfd-${local.env}-cloudwatch-alarms"
  display_name      = "BFD Cloudwatch Alarm. Created by Terraform."
  kms_master_key_id = data.aws_kms_key.master_key.id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms" {
  arn    = aws_sns_topic.cloudwatch_alarms.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms.arn, aws_sns_topic.cloudwatch_alarms.arn)
}

resource "aws_sns_topic_subscription" "alarm" {
  count                  = local.enable_victor_ops ? 1 : 0
  protocol               = "https"
  topic_arn              = aws_sns_topic.cloudwatch_alarms.arn
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

resource "aws_sns_topic" "cloudwatch_ok" {
  name         = "bfd-${local.env}-cloudwatch-ok"
  display_name = "BFD Cloudwatch OK notifications. Created by Terraform."

  kms_master_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_sns_topic_policy" "cloudwatch_ok" {
  arn    = aws_sns_topic.cloudwatch_ok.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_ok.arn, aws_sns_topic.cloudwatch_ok.arn)
}

resource "aws_sns_topic_subscription" "ok" {
  count                  = local.enable_victor_ops ? 1 : 0
  topic_arn              = aws_sns_topic.cloudwatch_ok.arn
  protocol               = "https"
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_notices" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-notices"
  display_name      = "BFD Cloudwatch Alarms notices to #bfd-notices. Created by Terraform."
  kms_master_key_id = data.aws_kms_key.master_key.id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_notices" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_notices.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_notices.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_notices.arn)
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_test" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-test"
  display_name      = "BFD Cloudwatch Alarms alerts to #bfd-test. Created by Terraform."
  kms_master_key_id = data.aws_kms_key.master_key.id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_test" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_test.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_test.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_test.arn)
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_warnings" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-warnings"
  display_name      = "BFD Cloudwatch Alarms alerts to #bfd-warnings. Created by Terraform."
  kms_master_key_id = data.aws_kms_key.master_key.id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_warnings" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_warnings.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_warnings.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_warnings.arn)
}

resource "aws_sns_topic" "cloudwatch_alarms_slack_bfd_alerts" {
  name              = "bfd-${local.env}-cloudwatch-alarms-slack-bfd-alerts"
  display_name      = "BFD Cloudwatch Alarms alerts to #bfd-alerts. Created by Terraform."
  kms_master_key_id = data.aws_kms_key.master_key.id
}

resource "aws_sns_topic_policy" "cloudwatch_alarms_slack_bfd_alerts" {
  arn    = aws_sns_topic.cloudwatch_alarms_slack_bfd_alerts.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.cloudwatch_alarms_slack_bfd_alerts.arn, aws_sns_topic.cloudwatch_alarms_slack_bfd_alerts.arn)
}

## IAM policy, user, and attachment to allow external read-write access to ETL bucket
# NOTE: We only need this for production, however it is ok to
# provision these resources for all environments since the mechanism
# by which we control access is through a manually provisioned
# access key
resource "aws_iam_policy" "etl_rw_s3" {
  name        = "bfd-${local.env}-etl-rw-s3"
  description = "ETL read-write S3 policy"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ETLRWKMS",
      "Action": ["kms:Decrypt"],
      "Effect": "Allow",
      "Resource": ["${data.aws_kms_key.master_key.arn}"]
    },
    {
      "Sid": "ETLRWBucketList",
      "Action": ["s3:ListBucket"],
      "Effect": "Allow",
      "Resource": ["${data.aws_s3_bucket.etl.arn}"]
    },
    {
      "Sid": "ETLRWBucketActions",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": ["${data.aws_s3_bucket.etl.arn}/*"]
    }
  ]
}
EOF
}

resource "aws_iam_user" "etl" {
  name = "bfd-${local.env}-etl"
}

resource "aws_iam_group" "etl" {
  name = "bfd-${local.env}-etl"
  path = "/"
}

resource "aws_iam_group_membership" "etl" {
  name = "bfd-${local.env}-etl"

  users = [
    aws_iam_user.etl.name,
  ]

  group = aws_iam_group.etl.name
}

## S3 bucket, policy, and KMS key for medicare opt out data
#
module "medicare_opt_out" {
  source     = "../resources/s3_pii"
  env_config = local.env_config

  pii_bucket_config = {
    name        = "medicare-opt-out"
    log_bucket  = data.aws_s3_bucket.logs.id
    read_arns   = var.medicare_opt_out_config.read_roles
    write_accts = var.medicare_opt_out_config.write_accts
    admin_arns  = var.medicare_opt_out_config.admin_users
  }
}

## CloudWatch Log Groups
#

resource "aws_cloudwatch_log_group" "var_log_messages" {
  name       = "/bfd/${local.env}/var/log/messages"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "var_log_secure" {
  name       = "/bfd/${local.env}/var/log/secure"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_pipeline_messages_txt" {
  name       = "/bfd/${local.env}/bfd-pipeline/messages.txt"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_access_txt" {
  name       = "/bfd/${local.env}/bfd-server/access.txt"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_access_json" {
  name       = "/bfd/${local.env}/bfd-server/access.json"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_messages_json" {
  name       = "/bfd/${local.env}/bfd-server/messages.json"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_newrelic_agent" {
  name       = "/bfd/${local.env}/bfd-server/newrelic_agent.log"
  kms_key_id = data.aws_kms_key.master_key.arn
}

resource "aws_cloudwatch_log_group" "bfd_server_gc" {
  name       = "/bfd/${local.env}/bfd-server/gc.log"
  kms_key_id = data.aws_kms_key.master_key.arn
}
