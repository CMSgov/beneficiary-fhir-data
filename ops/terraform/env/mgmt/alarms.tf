locals {
  victor_ops_url                                = local.ssm_config["/bfd/common/victor_ops_url"]
  ec2_failing_instances_runbook_url             = local.ssm_config["/bfd/common/alarm_ec2_failing_instances_runbook_url"]
  ec2_instance_script_failing_start_runbook_url = local.ssm_config["/bfd/common/alarm_ec2_instance_script_failing_start_runbook_url"]
  lambda_error_stats_runbook_url                = local.ssm_config["/bfd/common/alarm_lambda_error_stats_runbook_url"]

  cloudwatch_sns_topic_policy_spec = <<-EOF
{
  "Version": "2008-10-17",
  "Id": "__default_policy_ID",
  "Statement": [
    {
      "Sid": "Allow_Publish_Alarms",
      "Effect": "Allow",
      "Principal": {
        "Service": ["cloudwatch.amazonaws.com"]
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

# SNS Topic for Alarm actions when Alarms transition from OK -> ALARM, indicating that the Alarm
# condition has been met and that something is wrong and needs attention from the on-call
resource "aws_sns_topic" "victor_ops_alert" {
  name              = "bfd-${local.env}-victor-ops-alert"
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "victor_ops_alert" {
  arn    = aws_sns_topic.victor_ops_alert.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.victor_ops_alert.arn, aws_sns_topic.victor_ops_alert.arn)
}

resource "aws_sns_topic_subscription" "victor_ops_alert" {
  protocol               = "https"
  topic_arn              = aws_sns_topic.victor_ops_alert.arn
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

# SNS Topic for Alarm actions when Alarms transition from ALARM -> OK, indicating that the Alarm
# condition has been resolved
resource "aws_sns_topic" "victor_ops_ok" {
  name              = "bfd-${local.env}-victor-ops-ok"
  kms_master_key_id = local.kms_key_id
}

resource "aws_sns_topic_policy" "victor_ops_ok" {
  arn    = aws_sns_topic.victor_ops_ok.arn
  policy = format(local.cloudwatch_sns_topic_policy_spec, aws_sns_topic.victor_ops_ok.arn, aws_sns_topic.victor_ops_ok.arn)
}

resource "aws_sns_topic_subscription" "victor_ops_ok" {
  topic_arn              = aws_sns_topic.victor_ops_ok.arn
  protocol               = "https"
  endpoint               = local.victor_ops_url
  endpoint_auto_confirms = true
}

resource "aws_cloudwatch_metric_alarm" "ec2_failing_instances" {
  alarm_name          = "bfd-${local.env}-ec2-failing-instances"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  datapoints_to_alarm = 1
  evaluation_periods  = 1
  threshold           = 1
  treat_missing_data  = "missing"

  alarm_description = join("", [
    "At least 1 (see Alarm value for exact number) EC2 instance is failing its status checks.\n",
    "See ${local.ec2_failing_instances_runbook_url} for instructions on resolving this alert."
  ])

  metric_query {
    period      = 60
    expression  = "SELECT SUM(StatusCheckFailed) FROM \"AWS/EC2\""
    id          = "q1"
    label       = "StatusCheckFailed Sum"
    return_data = true
  }

  alarm_actions = [aws_sns_topic.victor_ops_alert.arn]
  ok_actions    = [aws_sns_topic.victor_ops_ok.arn]
}

## BFD-3520
resource "aws_cloudwatch_log_metric_filter" "ec2_init_fail_count" {
  name           = local.init_fail_filter_name
  pattern        = local.init_fail_pattern
  log_group_name = local.log_groups.cloudinit_out

  metric_transformation {
    name          = local.init_fail_metric_name
    namespace     = local.this_metric_namespace
    value         = "1"
    default_value = "0"
  }
}

resource "aws_cloudwatch_metric_alarm" "ec2_init_fail" {
  alarm_name          = local.init_fail_alarm_name
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"

  metric_name = local.init_fail_filter_name
  namespace   = local.this_metric_namespace
  period      = 60

  statistic = "Sum"
  threshold = "0"

  alarm_actions = [aws_sns_topic.victor_ops_alert.arn]

  actions_enabled = true
  alarm_description = join("", [
    "At least 1 (see Alarm value for exact number) EC2 instance is failing startup steps.\n",
    "See ${local.ec2_instance_script_failing_start_runbook_url} for instructions on resolving this alert."
  ])
}

data "aws_sns_topic" "internal_alert_slack" {
  #FIXME: replace when slack alert is in mgmt
  name = "bfd-test-cloudwatch-alarms-slack-bfd-test"
}

resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "bfd-mgmt-lambda-error"
  comparison_operator = "GreaterThanThreshold"
  datapoints_to_alarm = 1
  evaluation_periods  = 1
  threshold           = 0
  actions_enabled     = true
  treat_missing_data  = "ignore"

  alarm_description = join("", [
    "Alarm that is defined to send alerts to the BFD-Warnings/Alerts slack channel on reported Lambda failures\n",
    "See ${local.lambda_error_stats_runbook_url} for instructions on investigating this alert."
  ])

  metric_name = "Errors"
  namespace   = "AWS/Lambda"
  period      = 60
  statistic   = "Sum"

  #FIXME: replace when slack alert is in mgmt
  alarm_actions = [data.aws_sns_topic.internal_alert_slack.arn]
}

