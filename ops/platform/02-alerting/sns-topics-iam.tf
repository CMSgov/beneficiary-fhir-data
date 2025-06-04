data "aws_iam_policy_document" "splunk_topic_logs" {
  statement {
    sid = "AllowLogControl"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:PutMetricFilter",
      "logs:PutRetentionPolicy"
    ]
    resources = [
      aws_cloudwatch_log_group.splunk_incident_success.arn,
      aws_cloudwatch_log_group.splunk_incident_failure.arn
    ]
  }
}

resource "aws_iam_policy" "splunk_topic_logs" {
  name = "${local.splunk_incident_topic}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${local.splunk_incident_topic} SNS Topic to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = data.aws_iam_policy_document.splunk_topic_logs.json
}

resource "aws_iam_role" "splunk_topic" {
  name                  = "${local.splunk_incident_topic}-role"
  path                  = local.iam_path
  description           = "Role for the ${local.splunk_incident_topic} SNS Topic"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["sns"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "splunk_topic" {
  for_each = {
    logs = aws_iam_policy.splunk_topic_logs.arn
  }

  role       = aws_iam_role.splunk_topic.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "slack_topic_logs" {
  for_each = local.slack_channel_to_topic

  statement {
    sid = "AllowLogControl"
    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "logs:PutMetricFilter",
      "logs:PutRetentionPolicy"
    ]
    resources = [
      "${aws_cloudwatch_log_group.slack_failure[each.key].arn}:*",
      "${aws_cloudwatch_log_group.slack_success[each.key].arn}:*"
    ]
  }
}

resource "aws_iam_policy" "slack_topic_logs" {
  for_each = local.slack_channel_to_topic

  name = "${each.value}-logs-policy"
  path = local.iam_path
  description = join("", [
    "Permissions for the ${each.value} SNS Topic to write to its corresponding ",
    "CloudWatch Log Group and Log Stream",
  ])

  policy = data.aws_iam_policy_document.slack_topic_logs[each.key].json
}

resource "aws_iam_role" "slack_topic" {
  for_each = local.slack_channel_to_topic

  name                  = "${each.value}-role"
  path                  = local.iam_path
  description           = "Role for the ${each.value} SNS Topic"
  assume_role_policy    = data.aws_iam_policy_document.service_assume_role["sns"].json
  permissions_boundary  = local.permissions_boundary_arn
  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "slack_topic" {
  for_each = {
    for k, v in local.slack_channel_to_topic
    : "${k}--logs" => {
      policy = aws_iam_policy.slack_topic_logs[k].arn
      role   = aws_iam_role.slack_topic[k].name
    }
  }

  role       = each.value.role
  policy_arn = each.value.policy
}


