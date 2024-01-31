resource "aws_iam_policy" "cloudwatch_metrics" {
  for_each = local.lambdas

  name        = "${each.value.full_name}-cloudwatch-metrics"
  description = "Permissions for the ${each.value.full_name} Lambda to put and get metric data"

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
  name = "${local.lambdas[local.lambda_update_slis].full_name}-s3"
  description = join("", [
    "Permissions for the ${local.lambdas[local.lambda_update_slis].full_name} Lambda to list and ",
    "get objects in the ${data.aws_s3_bucket.etl.id} S3 bucket in the Done/ and Incoming/ folders"
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
  for_each = local.lambdas

  name = "${each.value.full_name}-logs"
  description = join("", [
    "Permissions for the ${each.value.full_name} Lambda to write to its corresponding CloudWatch ",
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
        "arn:aws:logs:${local.region}:${var.account_id}:log-group:/aws/lambda/${each.value.full_name}:*"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_policy" "dynamodb" {
  name = "${local.lambdas[local.lambda_update_slis].full_name}-dynamodb"
  description = join("", [
    "Permissions for the ${local.lambdas[local.lambda_update_slis].full_name} Lambda to interact ",
    "with the ${aws_dynamodb_table.update_slis_rif_available.name} and ",
    "${aws_dynamodb_table.update_slis_load_available.name} DynamoDB tables"
  ])
  policy = <<-EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem"
      ],
      "Resource": [
        "${aws_dynamodb_table.update_slis_rif_available.arn}",
        "${aws_dynamodb_table.update_slis_load_available.arn}"
      ]
    },
    {
      "Effect": "Allow",
      "Action": ["kms:GenerateDataKey*", "kms:Decrypt", "kms:Encrypt"],
      "Resource": ["${local.kms_key_arn}"]
    }
  ]
}
EOF
}

resource "aws_iam_role" "lambda" {
  for_each = local.lambdas

  name        = each.value.full_name
  path        = "/"
  description = "Role for ${each.value.full_name} Lambda"

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

resource "aws_iam_role_policy_attachment" "lambda_policies_to_roles" {
  for_each = merge([for k, v in {
    "${local.lambda_update_slis}" = [
      aws_iam_policy.cloudwatch_metrics[local.lambda_update_slis],
      aws_iam_policy.logs[local.lambda_update_slis],
      aws_iam_policy.s3,
      aws_iam_policy.dynamodb
    ]
    "${local.lambda_repeater}" = [
      aws_iam_policy.cloudwatch_metrics[local.lambda_repeater],
      aws_iam_policy.logs[local.lambda_repeater],
    ]
  } : { for policy in v : "${policy.name}" => { role = aws_iam_role.lambda[k], policy = policy } }]...)

  role       = each.value.role.name
  policy_arn = each.value.policy.arn
}

resource "aws_iam_policy" "invoke_repeater" {
  name = "${local.lambdas[local.lambda_repeater].full_name}-scheduler-assumee-allow-lambda-invoke"
  description = join("", [
    "Permissions for EventBridge Scheduler assumed role to invoke the ",
    "${local.lambdas[local.lambda_repeater].full_name} Lambda"
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
  name = "${local.lambdas[local.lambda_repeater].full_name}-scheduler-assumee"
  path = "/"
  description = join("", [
    "Role for EventBridge Scheduler allowing permissions to invoke the ",
    "${local.lambdas[local.lambda_repeater].full_name} Lambda"
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
