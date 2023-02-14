# CPM (aka "Cloud Protection Manager") is a backup service provided by CMS that can be used to backup/replicate
# several aws services such as s3 buckets, rds/ebs snapshots, etc. However, these services should define (tag) cpm
# policies in their respective modules, not here. This module is only responsible for the bits that are shared across
# all services such as SNS topics for alerting when backups fail, daily backup reports, etc.

# cpm daily report
resource "aws_sns_topic" "cpm_daily_report" {
  name              = "CPM-Daily-Report"
  display_name      = "CpmDaily"
  kms_master_key_id = "alias/aws/sns"
  delivery_policy   = <<EOF
{
  "http": {
    "defaultHealthyRetryPolicy": {
      "minDelayTarget": 20,
      "maxDelayTarget": 20,
      "numRetries": 3,
      "numMaxDelayRetries": 0,
      "numNoDelayRetries": 0,
      "numMinDelayRetries": 0,
      "backoffFunction": "linear"
    },
    "disableSubscriptionOverrides": false
  }
}
EOF

}

resource "aws_sns_topic_policy" "cpm_daily_report" {
  arn    = aws_sns_topic.cpm_daily_report.arn
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Id": "CPM_Daily_Report_Policy",
  "Statement": [
    {
      "Sid": "Permissions_1",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "SNS:Publish",
        "SNS:RemovePermission",
        "SNS:SetTopicAttributes",
        "SNS:DeleteTopic",
        "SNS:ListSubscriptionsByTopic",
        "SNS:GetTopicAttributes",
        "SNS:Receive",
        "SNS:AddPermission",
        "SNS:Subscribe"
      ],
      "Resource": "${aws_sns_topic.cpm_daily_report.arn}",
      "Condition": {
        "StringEquals": {
          "AWS:SourceOwner": "${local.account_id}"
        }
      }
    },
    {
      "Sid": "PublishPermissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "${data.aws_ssm_parameter.cpm_aws_account_arn.value}",
          "arn:aws:iam::${local.account_id}:root"
        ]
      },
      "Action": "SNS:Publish",
      "Resource": "arn:aws:sns:us-east-1:${local.account_id}:CPM-Daily-Report"
    },
    {
      "Sid": "SubscribePermissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "SNS:Subscribe",
        "SNS:Receive"
      ],
      "Resource": "arn:aws:sns:us-east-1:${local.account_id}:CPM-Daily-Report"
    }
  ]
}
EOF
}

# cpm failure alerts
resource "aws_sns_topic" "cpm_failure_alerts" {
  name              = "CPM-Failure-Alerts"
  display_name      = "CpmFailure"
  kms_master_key_id = "alias/aws/sns"
  delivery_policy   = <<EOF
{
  "http": {
    "defaultHealthyRetryPolicy": {
      "minDelayTarget": 20,
      "maxDelayTarget": 20,
      "numRetries": 3,
      "numMaxDelayRetries": 0,
      "numNoDelayRetries": 0,
      "numMinDelayRetries": 0,
      "backoffFunction": "linear"
    },
    "disableSubscriptionOverrides": false
  }
}
EOF
}

resource "aws_sns_topic_policy" "cpm_failure_alerts" {
  arn    = aws_sns_topic.cpm_failure_alerts.arn
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Id": "CPM_Failure_Alert_Policy",
  "Statement": [
    {
      "Sid": "Permissions_1",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "SNS:Publish",
        "SNS:RemovePermission",
        "SNS:SetTopicAttributes",
        "SNS:DeleteTopic",
        "SNS:ListSubscriptionsByTopic",
        "SNS:GetTopicAttributes",
        "SNS:Receive",
        "SNS:AddPermission",
        "SNS:Subscribe"
      ],
      "Resource": "arn:aws:sns:us-east-1:${local.account_id}:CPM-Failure-Alerts",
      "Condition": {
        "StringEquals": {
          "AWS:SourceOwner": "${local.account_id}"
        }
      }
    },
    {
      "Sid": "PublishPermissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": [
          "arn:aws:iam::${local.account_id}:root",
           "${data.aws_ssm_parameter.cpm_aws_account_arn.value}"
        ]
      },
      "Action": "SNS:Publish",
      "Resource": "arn:aws:sns:us-east-1:${local.account_id}:CPM-Failure-Alerts"
    },
    {
      "Sid": "SubscribePermissions",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": [
        "SNS:Subscribe",
        "SNS:Receive"
      ],
      "Resource": "arn:aws:sns:us-east-1:${local.account_id}:CPM-Failure-Alerts"
    }
  ]
}
EOF
}
