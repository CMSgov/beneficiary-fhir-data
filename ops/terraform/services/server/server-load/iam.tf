# TODO: consider refactoring, hoisting IAM resources into a centrally managed environmental configuration for services
data "aws_iam_policy" "cloudwatch_agent_policy" {
  arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# AWS CloudWatch agent needs extra IAM permissions for x-ray
data "aws_iam_policy" "cloudwatch_agent_xray_policy" {
  arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_iam_policy" "lambda" {
  name        = "bfd-${local.env}-${local.service}-lambda-invocation"
  path = var.cloudtamer_iam_path
  description = "Allow invocation of locust worker ${local.service} 'node' lambda in ${local.env}"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "lambda:InvokeFunction"
            ],
            "Resource": [
              "${aws_lambda_function.node.arn}"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "ssm" {
  name        = "bfd-${local.env}-${local.service}-ssm-parameters"
  path = var.cloudtamer_iam_path
  description = "Permissions to /bfd/${local.env}/common, /bfd/${local.env}/server, and /bfd/mgmt/common/sensitive/user SSM hierarchies"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ssm:GetParametersByPath",
                "ssm:GetParameters",
                "ssm:GetParameter"
            ],
            "Resource": [
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/mgmt/common/sensitive/user/*",
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/sensitive/user/*",
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/common/*",
                "arn:aws:ssm:us-east-1:${local.account_id}:parameter/bfd/${local.env}/server/*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "kms" {
  name        = "bfd-${local.env}-${local.service}-kms"
  path = var.cloudtamer_iam_path
  description = "Permissions to decrypt using master and config KMS keys for ${local.env} and mgmt"
  policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Effect" : "Allow",
          "Action" : [
            "kms:Decrypt"
          ],
          "Resource" : concat(
            [
              "${local.kms_key_arn}",
              "${local.mgmt_kms_key_arn}",
            ],
            local.kms_config_key_arns,
            local.mgmt_kms_config_key_arns
          )
        }
      ]
    }
  )
}

resource "aws_iam_policy" "rds" {
  name        = "bfd-${local.env}-${local.service}-rds"
  path = var.cloudtamer_iam_path
  description = "Permissions to describe ${local.env} RDS clusters"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "rds:DescribeDBClusters"
            ],
            "Resource": [
                "arn:aws:rds:us-east-1:${local.account_id}:cluster:bfd-${local.env}-*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "logs" {
  name        = "bfd-${local.env}-${local.service}-logs"
  path = var.cloudtamer_iam_path
  description = "Permissions to create and write to bfd-${local.env}-${local.service} logs"
  policy      = <<-EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "logs:CreateLogGroup",
            "Resource": "arn:aws:logs:us-east-1:${local.account_id}:*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": [
                "arn:aws:logs:us-east-1:${local.account_id}:log-group:/aws/lambda/bfd-${local.env}-${local.service}:*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "sqs" {
  name        = "bfd-${local.env}-${local.service}-sqs"
  path = var.cloudtamer_iam_path
  description = "Permissions to use ${local.queue_name} SQS queue"
  policy      = <<-EOF
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
            "Resource": [
                "${aws_sqs_queue.this.arn}"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "kms:GenerateDataKey*"
            ],
            "Resource": [
                "${local.kms_key_arn}"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy" "ecr" {
  name        = "bfd-${local.env}-${local.service}-ecr"
  path = var.cloudtamer_iam_path
  description = "Allow ECR permissions to ${data.aws_ecr_repository.ecr_controller.name}"
  policy      = <<-EOF
{
   "Version":"2012-10-17",
   "Statement":[
      {
         "Sid":"ListImagesInRepository",
         "Effect":"Allow",
         "Action":[
            "ecr:ListImages"
         ],
         "Resource":"arn:aws:ecr:us-east-1:${local.account_id}:repository/${data.aws_ecr_repository.ecr_controller.name}"
      },
      {
         "Sid":"GetAuthorizationToken",
         "Effect":"Allow",
         "Action":[
            "ecr:GetAuthorizationToken",
            "ecr:DescribeRegistry"
         ],
         "Resource":"*"
      },
      {
         "Sid":"ReadRepositoryContents",
         "Effect":"Allow",
         "Action":[
            "ecr:BatchCheckLayerAvailability",
            "ecr:GetDownloadUrlForLayer",
            "ecr:GetRepositoryPolicy",
            "ecr:DescribeRepositories",
            "ecr:ListImages",
            "ecr:DescribeImages",
            "ecr:BatchGetImage"
         ],
         "Resource":"arn:aws:ecr:us-east-1:${local.account_id}:repository/${data.aws_ecr_repository.ecr_controller.name}"
      }
   ]
}
EOF
}

resource "aws_iam_role" "lambda" {
  name        = "bfd-${local.env}-${local.service}-lambda"
  path        = var.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  description = "Role for node lambda profile use for ${local.service} in ${local.env}"

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
    "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole",
    aws_iam_policy.ssm.arn,
    aws_iam_policy.kms.arn,
    aws_iam_policy.rds.arn,
    aws_iam_policy.logs.arn,
    aws_iam_policy.sqs.arn
  ]
}

resource "aws_iam_role" "ec2" {
  name        = "bfd-${local.env}-${local.service}-ec2"
  path        = var.cloudtamer_iam_path
  permissions_boundary = data.aws_iam_policy.permissions_boundary.arn
  description = "Role for ec2 profile use for ${local.service} in ${local.env}"

  assume_role_policy = <<-EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Effect": "Allow",
        "Principal": {
          "Service": "ec2.amazonaws.com"
        }
      }
    ]
  }
  EOF

  managed_policy_arns = [
    data.aws_iam_policy.cloudwatch_agent_policy.arn,
    data.aws_iam_policy.cloudwatch_agent_xray_policy.arn,
    aws_iam_policy.ssm.arn,
    aws_iam_policy.kms.arn,
    aws_iam_policy.rds.arn,
    aws_iam_policy.sqs.arn,
    aws_iam_policy.lambda.arn,
    aws_iam_policy.ecr.arn
  ]
}

resource "aws_iam_instance_profile" "this" {
  name = "bfd-${local.env}-${local.service}"
  role = aws_iam_role.ec2.name
  path = var.cloudtamer_iam_path
}
