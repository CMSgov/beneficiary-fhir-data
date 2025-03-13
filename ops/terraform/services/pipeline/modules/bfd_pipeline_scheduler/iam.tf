resource "aws_iam_policy" "s3" {
  name = "${local.lambda_full_name}-s3"
  path = local.cloudtamer_iam_path
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
        "${data.aws_s3_bucket.etl.arn}/Synthetic/Done/*",
        "${data.aws_s3_bucket.etl.arn}/Synthetic/Incoming/*",
        "${data.aws_s3_bucket.etl.arn}/Done/*",
        "${data.aws_s3_bucket.etl.arn}/Incoming/*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "autoscaling" {
  name = "${local.lambda_full_name}-autoscaling"
  path = local.cloudtamer_iam_path
  description = join("", [
    "Permissions for the ${local.lambda_full_name} Lambda to describe ASGs and their Scheduled ",
    "Actions and to put Scheduled Actions to the ${var.ccw_pipeline_asg_details.name} ASG"
  ])

  # Unfortunately, both describe actions do not support any sort of resource restriction or
  # conditions 
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowPutAndDeleteScheduledActionsForPipelineASG",
      "Effect": "Allow",
      "Action": [
        "autoscaling:PutScheduledUpdateGroupAction",
        "autoscaling:DeleteScheduledAction"
      ],
      "Resource": "${var.ccw_pipeline_asg_details.arn}"
    },
    {
      "Sid": "AllowDescribingScheduledActionsAndAutoScalingGroups",
      "Effect": "Allow",
      "Action": [
        "autoscaling:DescribeAutoScalingGroups",
        "autoscaling:DescribeScheduledActions"
      ],
      "Resource": "*"
    }
  ]
}
EOF
}


resource "aws_iam_policy" "logs" {
  name = "${local.lambda_full_name}-logs"
  path = local.cloudtamer_iam_path
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

resource "aws_iam_role" "this" {
  name        = local.lambda_full_name
  path        = local.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
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

  force_detach_policies = true
}

resource "aws_iam_role_policy_attachment" "s3_attach_lambda_role" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.s3.arn
}

resource "aws_iam_role_policy_attachment" "autoscaling_attach_lambda_role" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.autoscaling.arn
}

resource "aws_iam_role_policy_attachment" "logs_attach_lambda_role" {
  role       = aws_iam_role.this.name
  policy_arn = aws_iam_policy.logs.arn
}
