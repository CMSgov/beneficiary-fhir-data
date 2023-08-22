resource "aws_iam_policy" "cloudwatch_metrics" {
  name        = "${local.update_slis_lambda_full_name}-cloudwatch-metrics"
  description = "Permissions for the ${local.update_slis_lambda_full_name} Lambda to put and get metric data"

  # Unfortunately, neither GetMetricData nor PutMetricData support resource-level permissions, and
  # only PutMetricData supports the cloudwatch:namespace condition. This is why they're both so
  # permissive
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "cloudwatch:GetMetricData",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": "cloudwatch:PutMetricData",
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "cloudwatch:namespace": "${local.metrics_namespace}"
        }
      }
    }
  ]
}
EOF
}

resource "aws_iam_policy" "s3" {
  name = "${local.update_slis_lambda_full_name}-s3"
  description = join("", [
    "Permissions for the ${local.update_slis_lambda_full_name} Lambda to list and get objects in the ",
    "${data.aws_s3_bucket.etl.id} S3 bucket in the Done/ and Incoming/ folders"
  ])

  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:ListBucket"],
      "Resource": [
        "${data.aws_s3_bucket.etl.arn}",
        "${data.aws_s3_bucket.etl.arn}/Done/*",
        "${data.aws_s3_bucket.etl.arn}/Incoming/*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "logs" {
  name = "${local.update_slis_lambda_full_name}-logs"
  description = join("", [
    "Permissions for the ${local.update_slis_lambda_full_name} Lambda to write to its corresponding CloudWatch ",
    "Log Group and Log Stream"
  ])

  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "logs:CreateLogGroup",
      "Resource": "arn:aws:logs:${local.region}:${var.account_id}:*"
    },
    {
      "Effect": "Allow",
      "Action": ["logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": [
        "arn:aws:logs:${local.region}:${var.account_id}:log-group:/aws/lambda/${local.update_slis_lambda_full_name}:*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "sqs" {
  name = "${local.update_slis_lambda_full_name}-sqs"
  description = join("", [
    "Permissions for the ${local.update_slis_lambda_full_name} Lambda to send and receive messages from the ",
    "${aws_sqs_queue.this.name} SQS queue"
  ])
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:GetQueueUrl",
        "sqs:SendMessage",
        "sqs:DeleteMessage",
        "sqs:ReceiveMessage",
        "sqs:PurgeQueue"
      ],
      "Resource": ["${aws_sqs_queue.this.arn}"]
    },
    {
      "Effect": "Allow",
      "Action": ["kms:GenerateDataKey*", "kms:Decrypt"],
      "Resource": ["${local.kms_key_arn}"]
    }
  ]
}
EOF
}

resource "aws_iam_role" "this" {
  name        = local.update_slis_lambda_full_name
  path        = "/"
  description = "Role for ${local.update_slis_lambda_full_name} Lambda"

  assume_role_policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      }
    }
  ]
}
EOF

  managed_policy_arns = [
    aws_iam_policy.cloudwatch_metrics.arn,
    aws_iam_policy.s3.arn,
    aws_iam_policy.logs.arn,
    aws_iam_policy.sqs.arn
  ]
}

resource "aws_iam_policy" "scheduler" {
  name = "${local.repeater_lambda_full_name}-scheduler"
  description = join("", [
    "Permissions for ${local.repeater_lambda_full_name} to pass the ",
    "the ${aws_iam_role.scheduler_assume_role.name} role to EventBridge Scheduler"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = ["iam:PassRole"]
          Resource = [aws_iam_role.scheduler_assume_role.arn]
        }
      ]
    }
  )
}

resource "aws_iam_policy" "invoke_repeater" {
  name = "${local.repeater_lambda_full_name}-scheduler-assumee-allow-lambda-invoke"
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.repeater_lambda_full_name} Lambda"
  ])

  policy = jsonencode(
    {
      Version = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "lambda:InvokeFunction"
          Resource = aws_lambda_function.repeater.arn
        }
      ]
    }
  )
}

resource "aws_iam_role" "scheduler_assume_role" {
  name = "${local.repeater_lambda_full_name}-scheduler-assumee"
  path = "/"
  description = join("", [
    "Role for EventBridge Scheduler allowing permissions to invoke the ",
    "${local.repeater_lambda_full_name} Lambda"
  ])

  assume_role_policy = jsonencode(
    {
      Version = "2012-10-17",
      Statement = [
        {
          Action = "sts:AssumeRole",
          Effect = "Allow",
          Principal = {
            Service = "scheduler.amazonaws.com"
          }
        }
      ]
    }
  )

  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "invoke_repeater_policy_to_scheduler_assume_role" {
  role       = aws_iam_role.scheduler_assume_role.name
  policy_arn = aws_iam_policy.invoke_repeater.arn
}
