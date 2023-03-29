resource "aws_iam_policy" "s3" {
  name = "${local.lambda_full_name}-s3"
  description = join("", [
    "Permissions for the ${local.lambda_full_name} Lambda to list and get objects in the ",
    "${data.aws_s3_bucket.etl.id} S3 bucket in the root and Synthetic Done/ and Incoming/ folders"
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
  name = "${local.lambda_full_name}-logs"
  description = join("", [
    "Permissions for the ${local.lambda_full_name} Lambda to write to its corresponding CloudWatch ",
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
        "arn:aws:logs:${local.region}:${var.account_id}:log-group:/aws/lambda/${local.lambda_full_name}:*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "sqs" {
  name = "${local.lambda_full_name}-sqs"
  description = join("", [
    "Permissions for the ${local.lambda_full_name} Lambda to send messages to the ",
    "${local.jenkins_job_queue_name} SQS queue"
  ])
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:GetQueueUrl",
        "sqs:SendMessage"
      ],
      "Resource": ["${local.jenkins_job_queue_arn}"]
    },
    {
      "Effect": "Allow",
      "Action": ["kms:GenerateDataKey*"],
      "Resource": ["${local.mgmt_kms_key_arn}"]
    }
  ]
}
EOF
}

resource "aws_iam_role" "this" {
  name        = local.lambda_full_name
  path        = "/"
  description = "Role for ${local.lambda_full_name} Lambda"

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
    aws_iam_policy.s3.arn,
    aws_iam_policy.logs.arn,
    aws_iam_policy.sqs.arn
  ]
}
