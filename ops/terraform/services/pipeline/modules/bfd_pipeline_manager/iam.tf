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
