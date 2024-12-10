# TODO: Consider hoisting some of this logic out of the server-load module; BFD-1883 et al
resource "aws_sqs_queue" "this" {
  name                       = local.queue_name
  visibility_timeout_seconds = 0
}

resource "aws_sns_topic" "this" {
  name              = local.queue_name
  kms_master_key_id = local.kms_key_id
}

resource "aws_autoscaling_notification" "this" {
  topic_arn = aws_sns_topic.this.arn

  group_names = [
    #data.aws_autoscaling_group.asg.name,
    local.active_asg_this_env,
  ]

  notifications = [
    "autoscaling:EC2_INSTANCE_LAUNCH",
  ]
}

resource "aws_sns_topic_subscription" "this" {
  topic_arn = aws_sns_topic.this.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.this.arn
}

resource "aws_sqs_queue_policy" "this" {
  queue_url = aws_sqs_queue.this.id

  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Id": "${local.queue_name}-broker-sns-to-sqs",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "sns.amazonaws.com"
      },
      "Action": "SQS:SendMessage",
      "Resource": "${aws_sqs_queue.this.arn}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${aws_sns_topic.this.arn}"
        }
      }
    }
  ]
}
EOF
}
